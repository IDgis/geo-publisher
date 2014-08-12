package nl.idgis.publisher.provider.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.provider.database.messages.Query;
import nl.idgis.publisher.provider.protocol.database.Column;
import nl.idgis.publisher.provider.protocol.database.DescribeTable;
import nl.idgis.publisher.provider.protocol.database.FetchTable;
import nl.idgis.publisher.provider.protocol.database.PerformCount;
import nl.idgis.publisher.provider.protocol.database.Record;
import nl.idgis.publisher.provider.protocol.database.Records;
import nl.idgis.publisher.provider.protocol.database.TableDescription;
import nl.idgis.publisher.provider.protocol.database.TableNotFound;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class Database extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final String driver, url, user, password;
	
	private Connection connection;
	private ActorRef content;
	
	public Database(String driver, String url, String user, String password) {
		this.driver = driver;
		this.url = url;
		this.user = user;
		this.password = password;
	}
	
	public static Props props(String driver, String url, String user, String password) {
		return Props.create(Database.class, driver, url, user, password);
	}
	
	@Override
	public void preStart() throws SQLException, ClassNotFoundException {
		if(driver != null) {
			Class.forName(driver);
		}
		connection = DriverManager.getConnection(url, user, password);
		
		content = getContext().actorOf(DatabaseContent.props(connection), "content");
	}
	
	@Override
	public void postStop() throws SQLException {
		connection.close();
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof FetchTable) {
			handleFetchTable((FetchTable)msg);
		} else if(msg instanceof DescribeTable) {
			handleDescribeTable((DescribeTable)msg);
		} else if(msg instanceof PerformCount) {
			handlePerformCount((PerformCount)msg);
		} else {
			unhandled(msg);
		}
	}

	private void handlePerformCount(PerformCount msg) {
		String sql = "select count(*) from " + msg.getTableName();
		
		final ActorRef sender = getSender();
		Patterns.ask(content, new Query(sql), 15000)
			.onSuccess(new OnSuccess<Object>() {

				@Override
				public void onSuccess(Object msg) throws Throwable {
					List<Record> records = ((Records)msg).getRecords();
					
					long count = 0;
					
					for(Record record : records) {
						count = ((Number)record.getValues().get(0)).longValue();
					}
					
					sender.tell(count, getSelf());
				}
				
			}, getContext().dispatcher());
	}

	private void handleDescribeTable(DescribeTable msg) {
		String requestedTableName = msg.getTableName();
		
		final String sql;
		int separatorIndex = requestedTableName.indexOf(".");
		if(separatorIndex == -1) {
			sql = "select column_name, data_type from user_tab_columns "
					+ "where table_name = '" + requestedTableName.toUpperCase() + "' "
					+ "order by column_id";
		} else {
			String owner = requestedTableName.substring(0, separatorIndex);
			String tableName = requestedTableName.substring(separatorIndex + 1);
			
			sql = "select column_name, data_type from all_tab_columns "
					+ "where owner = '" + owner.toUpperCase() + "' and table_name = '" + tableName.toUpperCase() 
					+ "' " + "order by column_id";
		}
		
		final ActorRef sender = getSender();
		Patterns.ask(content, new Query(sql), 15000)
			.onSuccess(new OnSuccess<Object>() {

				@Override
				public void onSuccess(Object msg) throws Throwable {
					List<Record> records = ((Records)msg).getRecords();
					
					int recordCount = records.size();
					if(recordCount == 0) {
						sender.tell(new TableNotFound(), getSelf());
					} else {					
						ArrayList<Column> columns = new ArrayList<>();
						for(Record record : records) {
							List<Object> values = record.getValues();
							
							String name = (String) values.get(0);
							String typeName = (String) values.get(1);
							
							Type type;
							switch(typeName.toUpperCase()) {
								case "NUMBER":
									type = Type.NUMERIC;
									break;
								case "DATE":
									type = Type.DATE;
									break;
								case "VARCHAR2":
								case "NVARCHAR2":
									type = Type.TEXT;
									break;
								case "SDO_GEOMETRY":
									type = Type.GEOMETRY;
									break;
								default:
									log.debug("unknown data type: " + typeName);
									continue;
							}
							
							columns.add(new Column(name, type));
						}
						
						sender.tell(new TableDescription(columns.toArray(new Column[columns.size()])), getSelf());
					}
				}
				
			}, getContext().dispatcher());
	}

	private void handleFetchTable(FetchTable msg) {
		log.debug("fetch table: " + msg);
		
		StringBuilder sb = new StringBuilder("select ");
		
		String separator = "";
		for(String columnName : msg.getColumnNames()) {
			sb.append(separator);
			sb.append(columnName);
			
			separator = ", ";
		}
		
		sb.append(" from ");
		sb.append(msg.getTableName());
		
		content.tell(new Query(sb.toString(), msg.getMessageSize()), getSender());
	}	
}

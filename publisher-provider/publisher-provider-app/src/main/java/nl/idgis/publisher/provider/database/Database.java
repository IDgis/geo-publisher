package nl.idgis.publisher.provider.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import scala.concurrent.Future;
import nl.idgis.publisher.provider.database.messages.Query;
import nl.idgis.publisher.provider.protocol.database.Column;
import nl.idgis.publisher.provider.protocol.database.DescribeTable;
import nl.idgis.publisher.provider.protocol.database.FetchTable;
import nl.idgis.publisher.provider.protocol.database.Record;
import nl.idgis.publisher.provider.protocol.database.TableDescription;
import nl.idgis.publisher.provider.protocol.database.TableNotFound;
import nl.idgis.publisher.provider.protocol.database.Type;
import nl.idgis.publisher.stream.StreamAggregator;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Mapper;
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
			content.tell(new Query("select * from " + ((FetchTable)msg).getTableName()), getSender());
		} else if(msg instanceof DescribeTable) {
			String requestedTableName = ((DescribeTable) msg).getTableName();
			
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
			
			Future<ArrayList<Record>> records = StreamAggregator.ask(getContext(), content, new Query(sql), new ArrayList<Record>(), 15000);			
			Future<Object> response = records.map(new Mapper<ArrayList<Record>, Object>() {
						
						@Override
						public Object apply(ArrayList<Record> records) {
							int recordCount = records.size();
							if(recordCount == 0) {
								return new TableNotFound();
							} 
							
							ArrayList<Column> columns = new ArrayList<>();
							for(Record record : records) {
								List<Object> values = record.getValues();
								
								String name = (String) values.get(0);
								String typeName = (String) values.get(1);
								
								Type type;
								switch(typeName.toUpperCase()) {
									case "NUMBER":
										type = Type.NUMBER;
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
							
							return new TableDescription(columns.toArray(new Column[columns.size()]));				
						}
					}, getContext().dispatcher());
			
			Patterns.pipe(response, getContext().dispatcher())
				.pipeTo(getSender(), getSelf());
		} else {
			unhandled(msg);
		}
	}	
}

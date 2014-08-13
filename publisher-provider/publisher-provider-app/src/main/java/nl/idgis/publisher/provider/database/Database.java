package nl.idgis.publisher.provider.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.provider.database.messages.Query;
import nl.idgis.publisher.provider.protocol.database.Column;
import nl.idgis.publisher.provider.protocol.database.DescribeTable;
import nl.idgis.publisher.provider.protocol.database.FetchTable;
import nl.idgis.publisher.provider.protocol.database.PerformCount;
import nl.idgis.publisher.provider.protocol.database.TableDescription;
import nl.idgis.publisher.provider.protocol.database.TableNotFound;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Database extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final String driver, url, user, password;
	
	private Connection connection;
	private ActorRef streamProvider;
	
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
		
		streamProvider = getContext().actorOf(DatabaseStreamProvider.props(connection), "streamProvider");
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

	private void handlePerformCount(PerformCount msg) throws SQLException {
		String sql = "select count(*) from " + msg.getTableName();
		
		Statement stmt = connection.createStatement();
		
		ResultSet rs = stmt.executeQuery(sql);
		rs.next();
		
		getSender().tell(rs.getLong(1), getSelf());
		
		rs.close();		
		stmt.close();	
	}

	private void handleDescribeTable(DescribeTable msg) throws SQLException {
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
		
		Statement stmt = connection.createStatement();
		
		ArrayList<Column> columns = new ArrayList<>();
		
		ResultSet rs = stmt.executeQuery(sql);
		while(rs.next()) {
			String name = rs.getString(1);
			String typeName = rs.getString(2);
			
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
		
		rs.close();		
		stmt.close();
		
		if(columns.isEmpty()) {
			getSender().tell(new TableNotFound(), getSelf());
		} else {
			getSender().tell(new TableDescription(columns.toArray(new Column[columns.size()])), getSelf());
		}
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
		
		streamProvider.tell(new Query(sb.toString(), msg.getMessageSize()), getSender());
	}	
}

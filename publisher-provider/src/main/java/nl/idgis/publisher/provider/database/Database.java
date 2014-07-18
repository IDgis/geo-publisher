package nl.idgis.publisher.provider.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import scala.concurrent.Future;
import nl.idgis.publisher.protocol.database.Column;
import nl.idgis.publisher.protocol.database.DescribeTable;
import nl.idgis.publisher.protocol.database.FetchTable;
import nl.idgis.publisher.protocol.database.Record;
import nl.idgis.publisher.protocol.database.TableDescription;
import nl.idgis.publisher.protocol.stream.StreamAggregator;
import nl.idgis.publisher.provider.database.messages.Query;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Mapper;
import akka.pattern.Patterns;

public class Database extends UntypedActor {	
	
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
			Future<TableDescription> tableDescription = records.map(new Mapper<ArrayList<Record>, TableDescription>() {
						
						@Override
						public TableDescription apply(ArrayList<Record> records) {
							Column[] columns = new Column[records.size()];
							
							int i = 0;
							for(Record record : records) {
								List<Object> values = record.getValues();
								columns[i++] = new Column((String)values.get(0), (String)values.get(1));
							}
							
							return new TableDescription(columns);				
						}
					}, getContext().dispatcher());
			
			Patterns.pipe(tableDescription, getContext().dispatcher())
				.pipeTo(getSender(), getSelf());
		} else {
			unhandled(msg);
		}
	}	
}

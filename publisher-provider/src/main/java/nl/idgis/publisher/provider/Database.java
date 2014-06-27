package nl.idgis.publisher.provider;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import nl.idgis.publisher.protocol.database.FetchTable;
import nl.idgis.publisher.protocol.database.Record;
import nl.idgis.publisher.protocol.database.SpecialValue;
import nl.idgis.publisher.protocol.stream.SyncStreamProvider;

import akka.actor.Props;

public class Database extends SyncStreamProvider<ResultSet, FetchTable, Record> {
	
	private Connection connection;
	
	private final String driver, url, user, password;
	
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
	}
	
	@Override
	public void postStop() throws SQLException {
		connection.close();
	}

	@Override
	protected ResultSet start(FetchTable msg) throws SQLException {		
		Statement stmt = connection.createStatement();
		return stmt.executeQuery("select * from " + msg.getTableName());
	}

	@Override
	protected boolean hasNext(ResultSet u) throws SQLException {
		return u.next();
	}

	@Override
	protected Record syncNext(ResultSet rs) throws Exception {
		Object[] values = new Object[rs.getMetaData().getColumnCount()];
		for(int i = 0; i < values.length; i++) {
			Object o = rs.getObject(i + 1);
			if(o instanceof Number || o instanceof String) {
				values[i] = o;
			} else {
				values[i] = new SpecialValue();
			}
		}		

		return new Record(values);
	}
	
	@Override
	public void stop(ResultSet rs) throws SQLException {
		rs.getStatement().close();
		rs.close();
	}
}

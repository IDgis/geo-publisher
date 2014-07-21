package nl.idgis.publisher.database;

import java.sql.Connection;
import java.sql.DriverManager;

import nl.idgis.publisher.utils.ConfigUtils;

import com.typesafe.config.Config;

import akka.actor.UntypedActor;

public abstract class JdbcDatabase extends UntypedActor {
	
	private final String driver, url, user, password;
	
	protected Connection connection;
	
	public JdbcDatabase(Config config) {
		driver = ConfigUtils.getOptionalString(config, "driver");
		url = config.getString("url");
		user = config.getString("user");
		password = config.getString("password");
	}
	
	@Override
	public void preStart() throws Exception {
		if(driver != null) {
			Class.forName(driver);
		}		
		
		connection = DriverManager.getConnection(url, user, password);
	}
	
	@Override
	public void postStop() throws Exception {
		connection.close();
	}
	
	protected void answer(Object msg) {
		getSender().tell(msg, getSelf());
	}
}

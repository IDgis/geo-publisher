package nl.idgis.publisher.provider.database;

import java.sql.Connection;

import nl.idgis.publisher.database.JdbcDatabase;

import akka.actor.Props;

import com.typesafe.config.Config;

public class Database extends JdbcDatabase {
	
	public Database(Config config) {
		super(config, "database");
	}
	
	public static Props props(Config config) {
		return Props.create(Database.class, config);
	}
	
	@Override
	protected Props createTransaction(Connection connection) {
		return Props.create(DatabaseTransaction.class, connection);
	}	
}

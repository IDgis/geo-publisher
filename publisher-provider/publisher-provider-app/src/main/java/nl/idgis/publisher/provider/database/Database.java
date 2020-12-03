package nl.idgis.publisher.provider.database;

import java.sql.Connection;

import nl.idgis.publisher.database.JdbcDatabase;

import akka.actor.Props;

import com.typesafe.config.Config;

public class Database extends JdbcDatabase {
	
	public Database(Config config, String name) {
		super(config, name + "-database");
	}
	
	public static Props props(Config config, String name) {
		return Props.create(Database.class, config, name);
	}
	
	@Override
	protected Props createTransaction(Connection connection) {
		Props props;

		// Value should come from config
		String databasetype_from_config = "oracle";

		/* Return props from Oracle or Postgis*/
		if ("oracle".equalsIgnoreCase(databasetype_from_config)) {
			props = OracleDatabaseTransaction.props(config, connection);
		} else {
			props = PostgresDatabaseTransaction.props(config, connection);
		}

		return props;
	}	
}

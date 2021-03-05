package nl.idgis.publisher.provider.database;

import java.sql.Connection;

import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.ConfigException;
import nl.idgis.publisher.database.JdbcDatabase;

import akka.actor.Props;

import com.typesafe.config.Config;

public class Database extends JdbcDatabase {

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public Database(Config config, String name) {
		super(config, name + "-database");
	}
	
	public static Props props(Config config, String name) {
		return Props.create(Database.class, config, name);
	}
	
	@Override
	protected Props createTransaction(Connection connection) throws Exception {
		Props props;

		// Value should come from config
		String vendor = config.getString("vendor");
		log.debug(String.format("Using database: %s", vendor));

		/* Return props from Oracle or postgres */
		if ("oracle".equalsIgnoreCase(vendor)) {
			props = OracleDatabaseTransaction.props(config, connection);
		} else if ("postgres".equalsIgnoreCase(vendor)) {
			props = PostgresDatabaseTransaction.props(config, connection);
		} else {
			// TODO
			// implement error
			log.error("Vendor is not supported");
			throw new ConfigException.BadValue("database{ vendor }", "Vendor not supported");
		}

		return props;
	}	
}

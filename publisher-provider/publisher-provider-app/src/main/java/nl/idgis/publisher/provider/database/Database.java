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
	protected Props createTransaction(Connection connection) throws ConfigException {
        DatabaseType databaseVendor;

        if (config.hasPath("vendor")) {
            try {
                databaseVendor = DatabaseType.valueOf(config.getString("vendor").toUpperCase());
            } catch(IllegalArgumentException iae) {
                throw new ConfigException.BadValue("vendor", "Invalid vendor supplied in config");
            }
        } else {
            databaseVendor = DatabaseType.ORACLE;
        }

        log.debug(String.format("Using database: %s", databaseVendor.toString()));

		/* Return props from Oracle or postgres */
        switch(databaseVendor) {
            case ORACLE:
                return OracleDatabaseTransaction.props(config, connection);
            case POSTGRES:
                return PostgresDatabaseTransaction.props(config, connection);
            default:
                log.error("Vendor is not supported");
                throw new ConfigException.BadValue("database {vendor}", "Invalid vendor supplied in config");
        }
	}	
}

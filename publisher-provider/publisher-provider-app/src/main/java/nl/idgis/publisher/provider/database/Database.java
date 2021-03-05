package nl.idgis.publisher.provider.database;

import java.sql.Connection;

import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.ConfigException;
import nl.idgis.publisher.database.JdbcDatabase;

import akka.actor.Props;

import com.typesafe.config.Config;
import nl.idgis.publisher.provider.sde.SDEType;

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
        SDEType databaseVendor;

		// Value should come from config
		String vendor = config.getString("vendor");
		log.debug(String.format("Using database: %s", vendor));

        try {
            databaseVendor = SDEType.valueOf(config.getString("vendor").toUpperCase());
        } catch(IllegalArgumentException iae) {
            throw new ConfigException.BadValue("database {vendor}", "Invalid vendor supplied in config");
        }


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

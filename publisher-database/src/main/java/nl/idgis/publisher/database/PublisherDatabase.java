package nl.idgis.publisher.database;

import java.sql.Connection;

import akka.actor.Props;

import com.typesafe.config.Config;

public class PublisherDatabase extends JdbcDatabase {

	public PublisherDatabase(Config config) {
		super(config, "publisher");		
	}
	
	public static Props props(Config config) {
		return Props.create(PublisherDatabase.class, config);
	}

	@Override
	protected Props createTransaction(Connection connection) {		
		return PublisherTransaction.props(config, connection);
	}
}

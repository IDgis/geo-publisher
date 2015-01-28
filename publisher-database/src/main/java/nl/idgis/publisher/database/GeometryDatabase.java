package nl.idgis.publisher.database;

import java.sql.Connection;

import com.typesafe.config.Config;

import akka.actor.Props;

public class GeometryDatabase extends JdbcDatabase {

	public GeometryDatabase(Config config) {
		super(config, "geometry");
	}
	
	public static Props props(Config config) {
		return Props.create(GeometryDatabase.class, config);
	}

	@Override
	protected Props createTransaction(Connection connection) {
		return GeometryTransaction.props(connection);
	}
	
}

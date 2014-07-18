package nl.idgis.publisher.provider.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import nl.idgis.publisher.protocol.database.Record;
import nl.idgis.publisher.protocol.stream.StreamProvider;
import nl.idgis.publisher.provider.database.messages.Query;

public class DatabaseContent extends StreamProvider<Query, Record> {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final Connection connection;
	
	private ActorRef converter;
	
	public DatabaseContent(Connection connection) {
		this.connection = connection;
	}
	
	public static Props props(Connection connection) {
		return Props.create(DatabaseContent.class, connection);
	}
	
	@Override
	public void preStart() throws Exception {
		converter = getContext().actorOf(OracleConverter.props(), "converter");
	}
	
	@Override
	protected Props start(Query msg) throws SQLException {
		String sql = msg.getSql();
		
		log.debug("executing query: " + sql);
		
		Statement stmt = connection.createStatement();
		return DatabaseCursor.props(stmt.executeQuery(sql), converter);
	}
}

package nl.idgis.publisher.provider.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.provider.database.messages.Query;
import nl.idgis.publisher.stream.StreamProvider;

public class DatabaseStreamProvider extends StreamProvider<Query> {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final Connection connection;
	
	private ActorRef converter;
	
	public DatabaseStreamProvider(Connection connection) {
		this.connection = connection;
	}
	
	public static Props props(Connection connection) {
		return Props.create(DatabaseStreamProvider.class, connection);
	}
	
	@Override
	public void preStart() throws Exception {
		converter = getContext().actorOf(OracleConverter.props(), "converter");
	}
	
	@Override
	protected Props start(Query msg) throws SQLException {
		String sql = msg.getSql();
		int messageSize = msg.getMessageSize();
		
		log.debug("executing query: " + sql, " message size: " + messageSize);
		
		Statement stmt = connection.createStatement();
		return DatabaseCursor.props(stmt.executeQuery(sql), messageSize, converter);
	}
}

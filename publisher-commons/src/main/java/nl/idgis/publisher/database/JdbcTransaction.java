package nl.idgis.publisher.database;

import java.sql.Connection;
import java.sql.SQLException;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.Rollback;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public abstract class JdbcTransaction extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Connection connection;
	
	public JdbcTransaction(Connection connection) {
		this.connection = connection;
	}
	
	protected abstract void executeQuery(JdbcContext context, Query query) throws Exception;
	
	@Override
	public void postStop() throws Exception {
		connection.close();
	};

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Commit) {
			log.debug("committing transaction");
			
			connection.commit();
			
			getSender().tell(new Ack(), getSelf());
			getContext().stop(getSelf());
		} else if(msg instanceof Rollback) {
			log.debug("rolling back transaction");
			
			connection.rollback();
			
			getSender().tell(new Ack(), getSelf());
			getContext().stop(getSelf());
		} else if(msg instanceof Query) {
			try {
				log.debug("executing query");
				Query query = (Query) msg;
				
				JdbcContext context = new JdbcContext(log, query, connection, getSender(), getSelf());
				executeQuery(context, query);						
				context.finish();
			} catch(SQLException e) {
				log.error(e, "query failure");
				
				getSender().tell(new Failure(e), getSelf());
				
				connection.close();
				getContext().stop(getSelf());
			}
		} else {
			unhandled(msg);
		}
	}
}

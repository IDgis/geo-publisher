package nl.idgis.publisher.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.NotFound;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.Rollback;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.stream.messages.NextItem;

import nl.idgis.publisher.utils.TypedIterable;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;

public abstract class JdbcTransaction extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	protected final Connection connection;
	
	private boolean answered = false;
	
	public JdbcTransaction(Connection connection) {
		this.connection = connection;
	}
	
	protected abstract void executeQuery(Query query) throws Exception;
	
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
				executeQuery((Query)msg);
				
				finish();
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
	
public static class Prepared {
		
		PreparedStatement stmt;
		
		private Prepared(PreparedStatement stmt) {
			this.stmt = stmt;
		}
		
		public void execute(Object... args) throws Exception {
			execute(Arrays.asList(args));
		}		
		
		public void execute(List<Object> args) throws Exception {
			execute(args, new Function<Object, Object>() {

				@Override
				public Object apply(Object o) throws Exception {
					return o;
				}				
			});
		}
		
		public void execute(List<Object> args, Function<Object, Object> converter) throws Exception {
			int i = 1;
			
			for(Object arg : args) {
				stmt.setObject(i++, converter.apply(arg));
			}
			
			stmt.execute();
			stmt.close();
		}
	}
	
	public <T> void answer(Class<T> type, Iterable<T> msg) {
		answer(new TypedIterable<>(type, msg));
	}
	
	public void answer(TypedIterable<?> msg) {
		answer((Object)msg);
	}	
	
	/**
	 * 
	 * @param msg
	 * 
	 * @deprecated use {@link #answer(Class, Iterable)} instead.
	 */
	@Deprecated
	public void answer(Iterable<?> msg) {
		answer((Object)msg);
	}
	
	private void answer() {
		if(answered) {
			throw new IllegalArgumentException("query already answered");
		}
		
		answered = true;
	}
	
	public void answerStreaming(ActorRef cursor) {
		answer();
		
		cursor.tell(new NextItem(), getSender());
	}
	
	public void answer(Object msg) {
		answer();
		
		Object response = msg == null ? new NotFound() : msg;
		
		getSender().tell(response, getSelf());		
	}
	
	public void execute(String sql) throws SQLException {
		Statement stmt = connection.createStatement();
		stmt.execute(sql);
		stmt.close();
	}
	
	public Prepared prepare(String sql) throws SQLException {
		return new Prepared(connection.prepareStatement(sql));
	}
	
	public void ack() {
		answer(new Ack());
	}
	
	void finish() {
		if(!answered) {
			throw new IllegalStateException("query not answered");
		}
	}
}

package nl.idgis.publisher.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.NotFound;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.Rollback;
import nl.idgis.publisher.database.messages.StreamingQuery;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.TypedIterable;
import nl.idgis.publisher.utils.TypedList;

import akka.actor.ActorRef;
import akka.actor.ReceiveTimeout;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;

public abstract class JdbcTransaction extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Duration receiveTimeout = Duration.apply(30, TimeUnit.SECONDS);
	
	protected final Connection connection;
	
	private boolean answered = false;
	
	private Set<ActorRef> cursors;
	
	protected JdbcTransaction(Connection connection) {
		this.connection = connection;
	}
	
	protected void executeQuery(Query query) throws Exception {
		unhandled(query);
	}
	
	protected void executeQuery(StreamingQuery query) throws Exception {
		unhandled(query);
	}
	
	@Override
	public final void postStop() throws Exception {
		log.debug("closing connection");
		
		connection.close();
	};
	
	protected void transactionPreStart() throws Exception {
		
	}
	
	@Override
	public final void preStart() throws Exception {
		getContext().setReceiveTimeout(receiveTimeout);
		
		cursors = new HashSet<>();
		
		transactionPreStart();
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		if(msg instanceof Commit) {
			handleCommit();
		} else if(msg instanceof Rollback) {
			handleRollback();
		} else if(msg instanceof Query) {
			handleQuery((Query)msg);
		} else if(msg instanceof StreamingQuery) {
			handleStreamingQuery((StreamingQuery)msg);
		} else if(msg instanceof ReceiveTimeout) {
			handleTimeout();
		} else if(msg instanceof Terminated) {
			handleTerminated((Terminated)msg);
		} else {
			unhandled(msg);
		}
	}

	private void handleTerminated(Terminated msg) {
		ActorRef actor = msg.getActor();
		
		if(cursors.remove(actor)) {
			log.debug("cursor terminated");
			
			if(cursors.isEmpty()) {
				log.debug("no cursors left");
				
				getContext().setReceiveTimeout(receiveTimeout);
			} else {
				log.debug("pending cursors");
			}
		} else {
			log.error("unknown actor terminated: " + actor);
		}
	}

	private void handleStreamingQuery(StreamingQuery msg) throws SQLException {
		try {	
			log.debug("executing streaming query");
			executeQuery(msg);				
			
			finish();
		} catch(Exception e) {
			failure(e);
		}
	}

	private void handleQuery(Query msg) throws SQLException {
		try {
			log.debug("executing query: {} from {}", msg, getSender());
			executeQuery(msg);
			
			finish();
		} catch(Exception e) {
			failure(e);
		}
	}

	private void handleRollback() throws SQLException {
		log.debug("rolling back transaction");
		
		try {
			connection.rollback();
			getSender().tell(new Ack(), getSelf());
		} catch(Exception e) {
			getSender().tell(new Failure(e), getSelf());
		}
		
		getContext().stop(getSelf());
	}

	private void handleCommit() throws SQLException {
		log.debug("committing transaction");
		
		try {
			connection.commit();
			getSender().tell(new Ack(), getSelf());
		} catch(Exception e) {
			getSender().tell(new Failure(e), getSelf());
		}
		
		getContext().stop(getSelf());
	}
	
	private void handleTimeout() {
		log.error("timeout");
		
		getContext().stop(getSelf());
	}

	private void failure(Exception e) throws SQLException {
		log.error(e, "query failure");
		
		getSender().tell(new Failure(e), getSelf());
		getContext().stop(getSelf());
	}
	
	protected static class Prepared {
		
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
	
	protected <T> void answer(Class<T> type, Iterable<T> msg) {
		answer(new TypedIterable<>(type, msg));
	}
	
	protected void answer(TypedIterable<?> msg) {
		answer((Object)msg);
	}
	
	protected <T> void answer(Class<T> type, List<T> msg) {
		answer(new TypedList<>(type, msg));
	}
	
	protected void answer(TypedList<?> msg) {
		answer((Object)msg);
	}
	
	/**
	 * 
	 * @param msg
	 * 
	 * @deprecated use {@link #answer(Class, Iterable)} instead.
	 */
	@Deprecated
	protected void answer(Iterable<?> msg) {
		answer((Object)msg);
	}
	
	private void answer() {
		if(answered) {
			throw new IllegalArgumentException("query already answered");
		}
		
		answered = true;
	}
	
	protected void answerStreaming(ActorRef cursor) {
		log.debug("answer streaming");
		
		answer();
		
		getContext().watch(cursor);
		cursors.add(cursor);
		
		getContext().setReceiveTimeout(Duration.Inf());
		
		cursor.tell(new NextItem(), getSender());
	}
	
	protected void answer(Object msg) {
		answer();
		
		Object response = msg == null ? new NotFound() : msg;
		
		getSender().tell(response, getSelf());		
	}
	
	protected void execute(String sql) throws SQLException {
		Statement stmt = connection.createStatement();
		stmt.execute(sql);
		stmt.close();
	}
	
	protected Prepared prepare(String sql) throws SQLException {
		return new Prepared(connection.prepareStatement(sql));
	}
	
	protected void ack() {
		answer(new Ack());
	}
	
	private void finish() {
		if(!answered) {
			throw new IllegalStateException("query not answered");
		}
		
		log.debug("query answered");
		
		answered = false;
	}
}

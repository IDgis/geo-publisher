package nl.idgis.publisher.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.ReceiveTimeout;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.NotFound;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.Rollback;
import nl.idgis.publisher.database.messages.StreamingQuery;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.stream.messages.NextItem;
import scala.concurrent.duration.Duration;

public abstract class JdbcTransaction extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Duration receiveTimeout = Duration.apply(30, TimeUnit.SECONDS);
	
	protected final Connection connection;
	
	protected final Config config;
	
	protected ExecutorService executorService;
		
	private Set<ActorRef> cursors;
	
	protected JdbcTransaction(Config config, Connection connection) {
		this.connection = connection;
		this.config = config;
	}
	
	protected Object executeQuery(Query query) throws Exception {
		return null;
	}
	
	protected ActorRef executeQuery(StreamingQuery query) throws Exception {
		return null;
	}
	
	@Override
	public final void postStop() throws Exception {
		
		if(config.hasPath("setApplicationName") && config.getBoolean("setApplicationName")) {
			String sql = "set application_name to publisher_idle";
			
			try(
				Statement stmt = connection.createStatement();
			) {
				stmt.execute(sql);
				connection.commit();
			}
		}
		
		log.debug("closing connection");
		connection.close();
		
		log.debug("shutting down executor service");
		executorService.shutdown();
		
		log.debug("stopped");
	};
	
	protected void transactionPreStart() throws Exception {
		
	}
	
	@Override
	public final void preStart() throws Exception {
		getContext().setReceiveTimeout(receiveTimeout);
		
		cursors = new HashSet<>();
		
		executorService = Executors.newSingleThreadExecutor();
		
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
			log.error("unknown actor terminated: {}", actor);
		}
	}

	private void handleStreamingQuery(StreamingQuery msg) throws SQLException {
		try {	
			log.debug("executing streaming query: {}", msg);
			ActorRef cursor = executeQuery(msg);
			if(cursor == null) {
				getSender().tell(new NotFound(), getSelf());
			} else {			
				getContext().watch(cursor);
				cursors.add(cursor);
				
				getContext().setReceiveTimeout(Duration.Inf());
				
				cursor.tell(new NextItem(), getSender());
			}
		} catch(Exception e) {
			log.error(e, "error during executing streaming query: {}", msg);
			getSender().tell(new Failure(e), getSelf());
		}
	}

	private void handleQuery(Query msg) throws SQLException {
		ActorRef sender = getSender(), self = getSelf();
		
		executorService.execute(() -> {
			try {
				log.debug("executing query: {} from {}", msg, sender);
				
				Object queryResult = executeQuery(msg);
				if(queryResult == null) {
					sender.tell(new NotFound(), self);
				} else {
					sender.tell(queryResult, self);
				}
			} catch(Exception e) {
				e.printStackTrace();
				
				log.error(e, "error during executing query: {}", msg);
				
				sender.tell(new Failure(e), self);
			}
		});
	}

	private void handleRollback() throws SQLException {
		log.debug("rolling back transaction");
		
		ActorRef sender = getSender(), self = getSelf();
		executorService.execute(() -> {
			try {
				connection.rollback();
				sender.tell(new Ack(), self);
			} catch(Exception e) {
				log.error(e, "rollback failed");
				
				sender.tell(new Failure(e), self);
			}
			
			self.tell(PoisonPill.getInstance(), self);
		});
	}

	private void handleCommit() throws SQLException {
		log.debug("committing transaction");
		
		ActorRef sender = getSender(), self = getSelf();
		executorService.execute(() -> {
			try {
				connection.commit();
				sender.tell(new Ack(), self);
			} catch(Exception e) {
				log.error(e, "commit failed");
				
				sender.tell(new Failure(e), self);
			}
			
			self.tell(PoisonPill.getInstance(), self);
		});
	}
	
	private void handleTimeout() {
		log.error("timeout");
		
		ActorRef self = getSelf();
		executorService.execute(() -> {
			try {
				connection.rollback();
			} catch(Exception e) {
				log.error(e, "rollback failed"); 
			}
			
			self.tell(PoisonPill.getInstance(), self);
		});
	}
}

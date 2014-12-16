package nl.idgis.publisher.database;

import java.sql.Connection;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.database.messages.StreamingQuery;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.utils.ConfigUtils;
import nl.idgis.publisher.utils.NameGenerator;

import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.actor.SupervisorStrategy.Directive;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;

import com.google.common.util.concurrent.ListenableFuture;
import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import com.typesafe.config.Config;

public abstract class JdbcDatabase extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final NameGenerator nameGenerator = new NameGenerator();
	
	private final String poolName;

	protected final Config config;
	
	private BoneCP connectionPool;
	
	private static class CreateTransaction {
		
		private final ActorRef sender;
		
		private final Connection connection;
		
		public CreateTransaction(ActorRef sender, Connection connection) {
			this.sender = sender;
			this.connection = connection;
		}
		
		public ActorRef getSender() {
			return sender;
		}
		
		public Connection getConnection() {
			return connection;
		}
	}
	
	public JdbcDatabase(Config config, String poolName) {
		this.config = config;
		this.poolName = poolName;
	}
	
	@Override
	public void preStart() throws Exception {
		String driver = ConfigUtils.getOptionalString(config, "driver");
		if(driver != null) {
			Class.forName(driver);
		}
		
		BoneCPConfig boneCpConfig = new BoneCPConfig();
		boneCpConfig.setJdbcUrl(config.getString("url"));
		boneCpConfig.setUsername(config.getString("user"));
		boneCpConfig.setPassword(config.getString("password"));
		boneCpConfig.setPoolName(poolName);
		
		log.debug("creating database pool");
		connectionPool = new BoneCP(boneCpConfig); 
	}
	
	@Override
	public void postStop() throws Exception {
		log.debug("closing pool closed");
		connectionPool.close();		
	}
	
	protected abstract Props createTransaction(Connection connection);
	
	@Override
	public final void onReceive(final Object msg) throws Exception {
		if(msg instanceof StartTransaction) {
			handleStartTransaction((StartTransaction)msg);
		} else if(msg instanceof Query) {			
			handleQuery((Query)msg);
		} else if(msg instanceof StreamingQuery) {
			handleStreamingQuery((StreamingQuery)msg);
		} else if(msg instanceof CreateTransaction) { // internal message sent by connection pool listener
			handleCreateTransaction((CreateTransaction)msg);
		} else {
			onReceiveNonQuery(msg);
		}
	}

	private void handleCreateTransaction(CreateTransaction msg) {
		log.debug("creating transaction");
		
		ActorRef transaction = getContext().actorOf(
				createTransaction(msg.getConnection()), 
				nameGenerator.getName("transaction"));
		
		msg.getSender().tell(new TransactionCreated(transaction), getSelf());
	}

	private void handleStreamingQuery(final StreamingQuery query) {
		log.debug("executing query in autocommit mode");
		
		ActorRef streamingAutoCommit = getContext().actorOf(
				StreamingAutoCommit.props(query, getSender()),
				nameGenerator.getName("streaming-auto-commit"));
		
		getSelf().tell(new StartTransaction(), streamingAutoCommit);
	}

	private void handleQuery(final Query query) {
		log.debug("executing query in autocommit mode");
		
		ActorRef autoCommit = getContext().actorOf(
				AutoCommit.props(query, getSender()), 
				nameGenerator.getName("auto-commit"));
		
		getSelf().tell(new StartTransaction(), autoCommit);
	}

	private void handleStartTransaction(StartTransaction msg) {
		final ActorRef sender = getSender();
		final ListenableFuture<Connection> connectionFuture = connectionPool.getAsyncConnection();
		connectionFuture.addListener(new Runnable() {

			@Override
			public void run() {
				final Connection connection; 
				try {
					connection = connectionFuture.get();
					log.debug("connection obtained from pool");
					
					connection.setAutoCommit(false);
					getSelf().tell(new CreateTransaction(sender, connection), getSelf());
				} catch (Exception e) {
					log.error(e, "couldn't obtain connection from pool");
					return;
				}
			}				
		}, getContext().dispatcher());
	}
	
	protected void onReceiveNonQuery(Object msg) throws Exception {
		unhandled(msg);
	}
	
	private final static SupervisorStrategy strategy = 
		new OneForOneStrategy(-1, Duration.Inf(), new Function<Throwable, Directive>() {
			@Override
			public Directive apply(Throwable t) {
				return OneForOneStrategy.stop();
			}
		});

	@Override
	public SupervisorStrategy supervisorStrategy() { 
		return strategy;
	}
}

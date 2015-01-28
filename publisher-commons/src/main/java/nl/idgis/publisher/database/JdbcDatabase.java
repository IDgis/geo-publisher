package nl.idgis.publisher.database;

import java.sql.Connection;
import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.database.messages.StreamingQuery;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.utils.ConfigUtils;
import nl.idgis.publisher.utils.UniqueNameGenerator;

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
	
	private static final int DEFAULT_POOL_SIZE = 10;
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
	
	private final String poolName;
	
	private final int poolSize;

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
		this(config, poolName, DEFAULT_POOL_SIZE);
	}
	
	public JdbcDatabase(Config config, String poolName, int poolSize) {
		this.config = config;
		this.poolName = poolName;
		this.poolSize = poolSize;
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
		boneCpConfig.setDefaultAutoCommit(false);
		boneCpConfig.setConnectionTimeout(1, TimeUnit.SECONDS);		
		boneCpConfig.setMaxConnectionsPerPartition(poolSize);
		
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
		
		Props transactionProps = createTransaction(msg.getConnection());
		ActorRef transaction = getContext().actorOf(
				transactionProps, 
				nameGenerator.getName(transactionProps.clazz()));
		
		msg.getSender().tell(new TransactionCreated(transaction), getSelf());
	}

	private void handleStreamingQuery(final StreamingQuery query) {
		log.debug("executing query in autocommit mode");
		
		ActorRef streamingAutoCommit = getContext().actorOf(
				StreamingAutoCommit.props(query, getSender()),
				nameGenerator.getName(StreamingAutoCommit.class, query.getClass()));
		
		getSelf().tell(new StartTransaction(), streamingAutoCommit);
	}

	private void handleQuery(final Query query) {
		log.debug("executing query in autocommit mode");
		
		ActorRef autoCommit = getContext().actorOf(
				AutoCommit.props(query, getSender()), 
				nameGenerator.getName(AutoCommit.class, query.getClass()));
		
		getSelf().tell(new StartTransaction(), autoCommit);
	}

	private void handleStartTransaction(StartTransaction msg) {
		final ActorRef sender = getSender();
		final ListenableFuture<Connection> connectionFuture = connectionPool.getAsyncConnection();
		connectionFuture.addListener(() -> {
			 
			try {
				Connection connection = connectionFuture.get();
				log.debug("connection obtained from pool");
				
				getSelf().tell(new CreateTransaction(sender, connection), getSelf());
			} catch (Exception e) {
				log.error("couldn't obtain connection from pool: {}", e);
				
				sender.tell(new Failure(e), getSelf());
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

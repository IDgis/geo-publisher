package nl.idgis.publisher.database;

import java.sql.Connection;

import scala.concurrent.duration.Duration;
import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.utils.ConfigUtils;
import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.actor.SupervisorStrategy.Directive;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import akka.pattern.Patterns;

import com.google.common.util.concurrent.ListenableFuture;
import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import com.typesafe.config.Config;

public abstract class JdbcDatabase extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final String poolName;
	protected final Config config;
	
	private BoneCP connectionPool;
	
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
			final ActorRef sender = getSender(), self = getSelf();
			final ListenableFuture<Connection> connectionFuture = connectionPool.getAsyncConnection();
			connectionFuture.addListener(new Runnable() {
	
				@Override
				public void run() {
					final Connection connection; 
					try {
						connection = connectionFuture.get();
						log.debug("connection obtained from pool");
						
						connection.setAutoCommit(false);
						final ActorRef transaction = getContext().actorOf(createTransaction(connection));
						sender.tell(new TransactionCreated(transaction), self);
					} catch (Exception e) {
						log.error(e, "couldn't obtain connection from pool");
						return;
					}
				}				
			}, getContext().dispatcher());
		} else if(msg instanceof Query) {
			log.debug("executing query in autocommit mode");
			
			final Query query = (Query)msg;			
			final ActorRef self = getSelf(), sender = getSender();
			
			Patterns.ask(self, new StartTransaction(), 15000)
				.onSuccess(new OnSuccess<Object>() {
					
					public void onSuccess(final Object msg) throws Throwable {
						log.debug("transaction created");
						
						final TransactionCreated tc = (TransactionCreated)msg;
						
						Patterns.ask(tc.getActor(), query, 15000)
							.onSuccess(new OnSuccess<Object>() {

								@Override
								public void onSuccess(final Object queryResponse) throws Throwable {
									log.debug("query executed");
									
									Patterns.ask(tc.getActor(), new Commit(), 15000)
										.onSuccess(new OnSuccess<Object>() {

											@Override
											public void onSuccess(Object msg) throws Throwable {
												log.debug("transaction completed");												
												sender.tell(queryResponse, self);
											}
										}, getContext().dispatcher());
								}
							}, getContext().dispatcher());
					}
			}, getContext().dispatcher());
		} else {
			unhandled(msg);
		}
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

package nl.idgis.publisher.database;

import java.sql.Connection;

import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.utils.ConfigUtils;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.google.common.util.concurrent.ListenableFuture;
import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import com.typesafe.config.Config;

public abstract class JdbcDatabase extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	protected final Config config;
	
	private BoneCP connectionPool;
	
	public JdbcDatabase(Config config) {
		this.config = config;
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
		
		log.debug("creating database pool");
		connectionPool = new BoneCP(boneCpConfig); 
	}
	
	@Override
	public void postStop() throws Exception {
		log.debug("closing pool closed");
		connectionPool.close();		
	}
	
	protected abstract Object executeQuery(Connection connection, Query msg) throws Exception;
	
	@Override
	public final void onReceive(final Object msg) throws Exception {
		if(msg instanceof Query) {
			final ActorRef sender = getSender(), self = getSelf();
			final ListenableFuture<Connection> connectionFuture = connectionPool.getAsyncConnection();
			connectionFuture.addListener(new Runnable() {
	
				@Override
				public void run() {
					final Connection connection; 
					try {
						connection = connectionFuture.get();
						log.debug("connection obtained from pool");
					} catch (Exception e) {
						log.error(e, "couldn't obtain connection from pool");
						return;
					}
					
					log.debug("executing query");
					try {						
						Object queryResult = executeQuery(connection, (Query) msg);
						log.debug("sending query result");
						sender.tell(queryResult, self);						
					} catch(Exception e) {
						log.error(e, "couldn't execute query");						
					}
					
					log.debug("releasing connection");
					try {
						connection.close();
					} catch (Exception e) {
						log.error(e, "couldn't release connection");
					}
				}				
			}, getContext().dispatcher());
		} else {
			unhandled(msg);
		}
	}	
}

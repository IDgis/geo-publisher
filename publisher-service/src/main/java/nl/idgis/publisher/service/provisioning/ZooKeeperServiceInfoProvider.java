package nl.idgis.publisher.service.provisioning;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.retry.ExponentialBackoffRetry;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class ZooKeeperServiceInfoProvider extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger (getContext ().system (), this);
	
	private final static String servicesBasePath = "/services/http";
	private final String zooKeeperHosts;
	private final String namespace;
	
	private CuratorFramework client = null;
	private TreeCache serviceTreeCache = null;
	
	public ZooKeeperServiceInfoProvider (final String zooKeeperHosts, final String namespace) {
		if (zooKeeperHosts == null) {
			throw new NullPointerException ("zooKeeperHosts cannot be null");
		}
		
		this.zooKeeperHosts = zooKeeperHosts;
		this.namespace = namespace;
	}
	
	public static Props props (final String zooKeeperHosts, final String namespace) {
		if (zooKeeperHosts == null) {
			throw new NullPointerException ("zooKeeperHosts cannot be null");
		}

		return Props.create (ZooKeeperServiceInfoProvider.class, zooKeeperHosts, namespace);
	}

	@Override
	public void preStart () throws Exception {
		log.info ("Listening for events from ZooKeeper cluster at " + zooKeeperHosts + " on path: " + servicesBasePath);
		
		client = CuratorFrameworkFactory
			.newClient (zooKeeperHosts, new ExponentialBackoffRetry (1000, 5));
		
		client.start ();
		
		serviceTreeCache = new TreeCache (client, servicesBasePath);
		
		serviceTreeCache.getListenable ().addListener ((client, event) -> {
			if (event.getData () == null || event.getData ().getPath () == null) {
				return;
			}
			
			final String path = event.getData ().getPath ();
			if (!path.startsWith (servicesBasePath + "/")) {
				return;
			}
			
			final String serviceId = path.substring (servicesBasePath.length () + 1);
			final int offset = serviceId.indexOf ('/');
			if (offset <= 0 || offset >= serviceId.length () - 1) {
				return;
			}
			
			final String serviceType = serviceId.substring (0, offset);

			if (TreeCacheEvent.Type.NODE_ADDED.equals (event.getType ())) {
				log.info ("Service added: " + serviceId);
				
			} else if (TreeCacheEvent.Type.NODE_REMOVED.equals (event.getType ())) {
				log.info ("Service removed: " + serviceId);
				
			} else if (TreeCacheEvent.Type.NODE_UPDATED.equals (event.getType ())) {
				log.info ("Service updated: " + serviceId);
				
			}
			// getSelf ().tell (msg, sender);
		});
		
		serviceTreeCache.start ();
	}
	
	@Override
	public void postStop () throws Exception {
		if (serviceTreeCache != null) {
			serviceTreeCache.close ();
			serviceTreeCache = null;
		}
		
		if (client != null) {
			client.close ();
			client = null;
		}
	}
	
	@Override
	public void onReceive (final Object msg) throws Exception {
		unhandled (msg);
	}
	
	// private void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception	
}

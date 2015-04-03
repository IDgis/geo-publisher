package nl.idgis.publisher.service.provisioning;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import nl.idgis.publisher.service.provisioning.messages.AddPublicationService;
import nl.idgis.publisher.service.provisioning.messages.AddStagingService;
import nl.idgis.publisher.service.provisioning.messages.RemovePublicationService;
import nl.idgis.publisher.service.provisioning.messages.RemoveStagingService;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;

public class ZooKeeperServiceInfoProvider extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger (getContext ().system (), this);

	private final static String servicesBasePath = "/services/http";
	private final ServiceInfo stagingServiceInfo;
	private final ActorRef target;
	private final String zooKeeperHosts;
	private final String namespace;
	private final String stagingEnvironmentId;
	private final Map<String, Service> activeServices = new HashMap<> ();
	
	private CuratorFramework client = null;
	private TreeCache serviceTreeCache = null;
	private boolean hasDefaultStaging = false;
	
	public ZooKeeperServiceInfoProvider (final ServiceInfo stagingServiceInfo, final ActorRef target, final String zooKeeperHosts, final String stagingEnvironmentId, final String namespace) {
		if (stagingServiceInfo == null) {
			throw new NullPointerException ("stagingServiceInfo cannot be null");
		}
		if (target == null) {
			throw new NullPointerException ("target cannot be null");
		}
		if (zooKeeperHosts == null) {
			throw new NullPointerException ("zooKeeperHosts cannot be null");
		}
		if (stagingEnvironmentId == null) {
			throw new NullPointerException ("stagingEnvironmentId cannot be null");
		}

		this.stagingServiceInfo = stagingServiceInfo;
		this.target = target;
		this.zooKeeperHosts = zooKeeperHosts;
		this.stagingEnvironmentId = stagingEnvironmentId;
		this.namespace = namespace;
	}
	
	public static Props props (final ServiceInfo stagingServiceInfo, final ActorRef target, final String zooKeeperHosts, final String stagingEnvironmentId, final String namespace) {
		if (stagingServiceInfo == null) {
			throw new NullPointerException ("stagingServiceInfo cannot be null");
		}
		if (target == null) {
			throw new NullPointerException ("target cannot be null");
		}
		if (zooKeeperHosts == null) {
			throw new NullPointerException ("zooKeeperHosts cannot be null");
		}
		if (stagingEnvironmentId == null) {
			throw new NullPointerException ("stagingEnvironmentId cannot be null");
		}

		return Props.create (ZooKeeperServiceInfoProvider.class, stagingServiceInfo, target, zooKeeperHosts, stagingEnvironmentId, namespace);
	}

	@Override
	public void preStart () throws Exception {
		log.info ("Listening for events from ZooKeeper cluster at " + zooKeeperHosts + " on path: " + servicesBasePath);
		
		final CuratorFramework clientWithoutNamespace = CuratorFrameworkFactory
			.newClient (zooKeeperHosts, new ExponentialBackoffRetry (1000, 5));
		
		if (namespace != null) {
			client = clientWithoutNamespace.usingNamespace (namespace);
		} else {
			client = clientWithoutNamespace;
		}
		
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
			final JsonNode urlNode = readJson (event.getData ().getData ()).path ("url");
			final String serviceUrl = urlNode.isMissingNode () ? null : urlNode.asText ();

			if (TreeCacheEvent.Type.NODE_ADDED.equals (event.getType ())) {
				getSelf ().tell (new Event (EventType.ADDED, serviceId, serviceType, serviceUrl), getSelf ());
			} else if (TreeCacheEvent.Type.NODE_REMOVED.equals (event.getType ())) {
				getSelf ().tell (new Event (EventType.REMOVED, serviceId, serviceType, serviceUrl), getSelf ());
			} else if (TreeCacheEvent.Type.NODE_UPDATED.equals (event.getType ())) {
				getSelf ().tell (new Event (EventType.UPDATED, serviceId, serviceType, serviceUrl), getSelf ());
			}
		});
		
		serviceTreeCache.start ();
		
		// Add the default staging environment:
		//log.info ("Adding default staging environment");
		//target.tell (new AddStagingService (stagingServiceInfo), getSelf ());
		//hasDefaultStaging = true;
	}
	
	private JsonNode readJson (final byte[] data) {
		if (data == null) {
			return MissingNode.getInstance ();
		}
		
		final String stringContent = new String (data, Charset.forName ("UTF-8"));
		
		try {
			return new ObjectMapper().readTree (stringContent);
		} catch (IOException e) {
			log.error (e, "Unable to read ZooKeeper service payload, ignoring.");
			return MissingNode.getInstance ();
		}
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
		if (msg instanceof Event) {
			final Event event = (Event) msg;
			
			log.info ("ZooKeeper event received: " + event.toString ());
		
			handleEvent (event);
		} else {
			unhandled (msg);
		}
	}
	
	private void handleEvent (final Event event) {
		final boolean isStaging = event.serviceType.equals (stagingEnvironmentId);
		
		// Remove the default staging environment:
		if (isStaging && hasDefaultStaging) {
			log.info ("Removing default staging environment");
			
			hasDefaultStaging = false;
			target.tell (new RemoveStagingService (stagingServiceInfo), getSelf ());
		}
		
		// Remove existing service infos:
		if (EventType.UPDATED.equals (event.eventType) || EventType.REMOVED.equals (event.eventType)) {
			final Service existingService = activeServices.get (event.serviceId);
			
			if (existingService == null) {
				log.error ("Received event of type " + event.eventType + ", but service " + event.serviceId + " isn't currently registered. Skipping event.");
				return;
			}
			
			final ServiceInfo serviceInfo = createServiceInfo (existingService);
			
			if (isStaging) {
				log.info ("Removing staging service: " + existingService.serviceUrl);
				target.tell (new RemoveStagingService (serviceInfo), getSelf ());
			} else {
				log.info ("Removing publication service: " + existingService.serviceUrl);
				target.tell (new RemovePublicationService (serviceInfo), getSelf ());
			}
			
			activeServices.remove (event.serviceId);
		}
		
		// Add new service infos:
		if (EventType.UPDATED.equals (event.eventType) || EventType.ADDED.equals (event.eventType)) {
			final Service newService = new Service (event.serviceType, event.serviceUrl);
			final ServiceInfo serviceInfo = createServiceInfo (newService);
			
			if (isStaging) {
				log.info ("Adding staging service: " + newService.serviceUrl);
				target.tell (new AddStagingService (serviceInfo), getSelf ());
			} else {
				log.info ("Adding publication service: " + newService.serviceUrl);
				target.tell (new AddPublicationService (event.serviceType, serviceInfo), getSelf ());
			}
			
			activeServices.put (event.serviceId, newService);
		}
	}
	
	private ServiceInfo createServiceInfo (final Service service) {
		final String url = service.serviceUrl.endsWith ("/") ? service.serviceUrl : service.serviceUrl + "/";
		
		return new ServiceInfo (
				new ConnectionInfo (
					url, 
					stagingServiceInfo.getService ().getUser (), 
					stagingServiceInfo.getService ().getPassword ()
				), 
				stagingServiceInfo.getDatabase ()
			);
	}

	private static enum EventType {
		ADDED,
		REMOVED,
		UPDATED
	}
	
	private final static class Event implements Serializable {
		private static final long serialVersionUID = 5859878851304575044L;
		
		public final EventType eventType;
		public final String serviceId;
		public final String serviceType;
		public final String serviceUrl;
		
		public Event (final EventType eventType, final String serviceId, final String serviceType, final String serviceUrl) {
			this.eventType = eventType;
			this.serviceId = serviceId;
			this.serviceType = serviceType;
			this.serviceUrl = serviceUrl;
		}
		
		@Override
		public String toString () {
			return eventType.toString () + ": " + serviceId + ", " + serviceType + ", " + serviceUrl;
		}
	}
	
	private final static class Service implements Serializable {
		private static final long serialVersionUID = -2416854461822219733L;
		
		public final String serviceType;
		public final String serviceUrl;
		
		public Service (final String serviceType, final String serviceUrl) {
			this.serviceType = serviceType;
			this.serviceUrl = serviceUrl;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((serviceType == null) ? 0 : serviceType.hashCode());
			result = prime * result
					+ ((serviceUrl == null) ? 0 : serviceUrl.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Service other = (Service) obj;
			if (serviceType == null) {
				if (other.serviceType != null)
					return false;
			} else if (!serviceType.equals(other.serviceType))
				return false;
			if (serviceUrl == null) {
				if (other.serviceUrl != null)
					return false;
			} else if (!serviceUrl.equals(other.serviceUrl))
				return false;
			return true;
		}
	}
}

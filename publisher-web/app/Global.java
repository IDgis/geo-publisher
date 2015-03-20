import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import models.Domain;
import nl.idgis.publisher.domain.web.Filter;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.data.format.Formatters;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.mvc.Results;
import views.html.exceptions.domainAccessException;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Global extends GlobalSettings {

	private ZooKeeper zooKeeper;
	private AtomicBoolean zooKeeperRegistered = new AtomicBoolean (false);
	
	@Override
	public void onStart (final Application application) {
		Formatters.register (Filter.class, new Formatters.SimpleFormatter<Filter> () {
			@Override
			public Filter parse (final String text, final Locale locale) throws ParseException {
				try {
					return Json.fromJson (Json.parse (text), Filter.class);
				} catch (Exception e) {
					Logger.error ("Failed to parse filter expression", e);
					throw new ParseException (e.getMessage (), 0);
				}
			}

			@Override
			public String print (final Filter filter, final Locale locale) {
				return Json.stringify (Json.toJson (filter));
			}
		});
		
		// Create a Zookeeper connection:
		final String zooKeeperHosts = application.configuration ().getString ("zooKeeper.hosts", null);
		if (zooKeeperHosts != null) {
			final int zooKeeperTimeout = application.configuration ().getInt ("zooKeeper.timeoutInMillis", 10000);
			
			final String applicationDomain = application.configuration ().getString ("application.domain", "localhost");
			final int httpPort = application.configuration ().getInt ("http.port", 9000);
			final String httpAddress = application.configuration ().getString ("http.address", "0.0.0.0");
			final String destinationIp;

			Logger.debug ("Http address: " + httpAddress);
			
			if ("0.0.0.0".equals (httpAddress)) {
				destinationIp = getPublicIp ();
			} else {
				destinationIp = httpAddress;
			}

			if (destinationIp == null) {
				Logger.error ("Failed to determine the public IP of this server, skipping Zookeeper registration.");
			} else {
				final ObjectNode configuration = Json.newObject ();
				final ArrayNode proxy = configuration.putArray ("proxy");
				final ObjectNode proxyLine = proxy.addObject ();
	
				proxyLine.put ("type", "http");
				proxyLine.put ("path", "/");
				proxyLine.put ("domain", applicationDomain);
				proxyLine.put ("destination", "http://" + destinationIp + ":" + httpPort + "/");
				
				final String zooKeeperConfiguration = Json.stringify (configuration);
				
				Logger.info ("Connecting to ZooKeeper cluster: " + zooKeeperHosts);
				Logger.info ("Sending ZooKeeper configuration: " + configuration);
				
				try {
					zooKeeper = new ZooKeeper (zooKeeperHosts, zooKeeperTimeout, (event) -> handleZooKeeperEvent (event, applicationDomain, zooKeeperConfiguration), false);
				} catch (IOException e) {
					zooKeeper = null;
					Logger.error ("Failed to connect to a ZooKeeper instance", e);
					throw new RuntimeException (e);
				}
			}
		}
	}
	
	private String getPublicIp () {
		try {
			final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces ();
			
			while (networkInterfaces.hasMoreElements ()) {
				final NetworkInterface networkInterface = networkInterfaces.nextElement ();
				
				// Skip loopback point-to-point interfaces and interfaces that are not up:
				if (networkInterface.isLoopback () || networkInterface.isPointToPoint () || !networkInterface.isUp ()) {
					continue;
				}
				
				final Enumeration<InetAddress> addresses = networkInterface.getInetAddresses ();
				while (addresses.hasMoreElements ()) {
					final InetAddress address = addresses.nextElement ();
					
					if (address.isLoopbackAddress () || address.isMulticastAddress ()) {
						continue;
					}
					
					if (address instanceof Inet4Address) {
						return address.getHostAddress ();
					}
				}
			}
			
			return null;
		} catch (SocketException e) {
			throw new RuntimeException ("Failed to determine public IP of this server.");
		}
	}

	@Override
	public void onStop (final Application app) {
		if (zooKeeper != null) {
			try {
				zooKeeper.close ();
			} catch (InterruptedException e) {
				Logger.warn ("Failed to close the ZooKeeper connection", e);
			}
		}
	}
	
	private void handleZooKeeperEvent (final WatchedEvent event, final String applicationDomain, final String configuration) {
		switch (event.getState ()) {
		case AuthFailed:
			break;
		case ConnectedReadOnly:
			break;
		case Disconnected:
			break;
		case Expired:
			break;
		case SaslAuthenticated:
			break;
		case SyncConnected:
			registerApplication (applicationDomain, configuration);
			break;
		default:
			break;
		}
		
	}
	
	private void registerApplication (final String applicationDomain, final String configuration) {
		
		if (!zooKeeperRegistered.compareAndSet (false, true)) {
			return;
		}
		
		try {
			createPublicPath ("/services");
			createPublicPath ("/services/web");
			createPublicPath ("/services/web/domains");
			createPublicPath ("/services/web/domains/" + applicationDomain);
			
			final String zooKeeperPath = zooKeeper.create (String.format ("/services/web/domains/%s/", applicationDomain), configuration.getBytes (Charset.forName ("UTF-8")), Ids.READ_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
			
			Logger.info ("Registered at path: " + zooKeeperPath);
		} catch (KeeperException | InterruptedException e) {
			Logger.error ("Failed to register this application with ZooKeeper", e);
			throw new RuntimeException (e);
		}
	}
	
	private void createPublicPath (final String path) throws InterruptedException, KeeperException {
		try {
			if (zooKeeper.exists (path, null) == null) {
				zooKeeper.create (path, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}
		} catch (KeeperException e) {
			if (!Code.NODEEXISTS.equals (e.code ())) {
				throw e;
			}
		}
	}
	
	@Override
    public F.Promise<Result> onError (final RequestHeader request, final Throwable t) {
		final Throwable cause = t != null ? t.getCause () : null;
		
    	if (cause != null && cause instanceof Domain.DomainAccessException) {
    		// Log the exception using toString so that the exception ID is logged.
    		Logger.error (t.toString (), t);
    		
    		// Extract the exception ID. Unfortunately, there is no API available to get the exception ID
    		// so we rely on toString returning the ID in a certain format.
    		final String msg = t.toString ();
    		final String id;
    		if (msg.startsWith ("@")) {
    			final int offset = msg.indexOf (':');
    			if (offset > 0) {
    				id = msg.substring (0, offset);
    			} else {
    				id = null;
    			}
    		} else {
    			id = null;
    		}
    		
    		return Promise.pure ((Result) Results.internalServerError (domainAccessException.render (id)));
    	}
    	
        return null;
    }
}

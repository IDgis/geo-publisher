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

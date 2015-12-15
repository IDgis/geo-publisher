package nl.idgis.publisher.service.geoserver.rest;

import akka.event.LoggingAdapter;

import nl.idgis.publisher.utils.FutureUtils;

public class DefaultGeoServerRestFactory implements GeoServerRestFactory {
	
	private static final long serialVersionUID = -5310136879361912716L;
	
	private final String serviceLocation, user, password;
	
	public DefaultGeoServerRestFactory(String serviceLocation, String user, String password) {
		this.serviceLocation = serviceLocation;
		this.user = user;
		this.password = password;
	}

	@Override
	public GeoServerRest create(FutureUtils f, LoggingAdapter log) throws Exception {
		return new DefaultGeoServerRest(f, log, serviceLocation, user, password);
	}
}

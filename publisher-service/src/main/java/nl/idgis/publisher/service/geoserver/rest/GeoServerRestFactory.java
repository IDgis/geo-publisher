package nl.idgis.publisher.service.geoserver.rest;

import java.io.Serializable;

import akka.event.LoggingAdapter;

import nl.idgis.publisher.utils.FutureUtils;

public interface GeoServerRestFactory extends Serializable {

	GeoServerRest create(FutureUtils f, LoggingAdapter log) throws Exception;

}

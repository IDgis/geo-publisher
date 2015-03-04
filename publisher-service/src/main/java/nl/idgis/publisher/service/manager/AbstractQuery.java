package nl.idgis.publisher.service.manager;

import java.util.concurrent.CompletableFuture;

import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.QGenericLayer;

public abstract class AbstractQuery<T> {
	
	protected final LoggingAdapter log;
	
	protected final static QGenericLayer child = new QGenericLayer("child"), parent = new QGenericLayer("parent");
	
	AbstractQuery(LoggingAdapter log) {
		this.log = log;
	}

	abstract CompletableFuture<T> result();
}

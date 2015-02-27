package nl.idgis.publisher.service.manager;

import java.util.concurrent.CompletableFuture;

import nl.idgis.publisher.database.QGenericLayer;

public abstract class AbstractQuery<T> {
	
	protected final static QGenericLayer child = new QGenericLayer("child"), parent = new QGenericLayer("parent");

	abstract CompletableFuture<T> result();
}

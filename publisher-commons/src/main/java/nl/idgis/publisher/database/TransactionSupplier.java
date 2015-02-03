package nl.idgis.publisher.database;

import java.util.concurrent.CompletableFuture;

public interface TransactionSupplier<T> {
	
	CompletableFuture<? extends Transaction<T>> transaction();
}
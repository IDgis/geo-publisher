package nl.idgis.publisher.database;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import akka.event.LoggingAdapter;

public class TransactionHandler<T> {
	
	private final TransactionSupplier<T> supplier;
	
	private final LoggingAdapter log;
	
	public TransactionHandler(TransactionSupplier<T> supplier, LoggingAdapter log) {
		this.supplier = supplier;
		this.log = log;
	}
	
	public <U> CompletableFuture<U> transactional(Function<T, CompletableFuture<U>> handler) {
		CompletableFuture<U> future = new CompletableFuture<U>();
		
		supplier.transaction().whenComplete((tx, t0) -> {
			if(t0 != null) {
				future.completeExceptionally(t0);
			} else {
				try {
					tx.apply(handler).whenComplete((t, t1) -> {
						log.debug("transaction handler completed");
						
						if(t1 != null) {
							log.error("future completed exceptionally: {}", t1);
							
							tx.rollback().whenComplete((a, t2) -> {
								if(t2 != null) {
									log.error("rollback failed: {}", t2);
								}
								
								future.completeExceptionally(t1);
							});
						} else {
							tx.commit().whenComplete((a, t2) -> {
								if(t2 != null) {
									log.error("commit failed: {}", t2);
									
									future.completeExceptionally(t2);
								} else {
									future.complete(t);
								}
							});
						}
					});
				} catch(Throwable t1) {
					log.error("transaction handler raised exception: {}", t1);
					
					tx.rollback().whenComplete((a, t2) -> {
						future.completeExceptionally(t1);
					});
				}
			}
		});
		
		return future;
	}
}
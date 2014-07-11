package nl.idgis.publisher.protocol.stream;

import akka.dispatch.Futures;
import scala.concurrent.Future;

public abstract class SyncStreamCursor<T, V extends Item> extends StreamCursor<T, V> {
	
	protected SyncStreamCursor(T t) {
		super(t);
	}
	
	protected abstract V syncNext() throws Exception;

	@Override
	protected final Future<V> next() {
		try {
			return Futures.successful(syncNext());
		} catch(Exception e) {
			return Futures.failed(e);
		}
	}
}

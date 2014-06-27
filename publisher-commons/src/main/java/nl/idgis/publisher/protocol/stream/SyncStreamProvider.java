package nl.idgis.publisher.protocol.stream;

import akka.dispatch.Futures;
import scala.concurrent.Future;

public abstract class SyncStreamProvider<T, U extends Start, V extends Item> extends StreamProvider<T, U, V> {

	protected abstract V syncNext(T t) throws Exception;

	@Override
	protected final Future<V> next(T t) {
		try {
			return Futures.successful(syncNext(t));
		} catch(Exception e) {
			return Futures.failed(e);
		}
	}
}

package nl.idgis.publisher.provider.mock;

import java.util.Iterator;

import nl.idgis.publisher.provider.protocol.database.Records;
import nl.idgis.publisher.stream.StreamCursor;

import scala.concurrent.Future;

import akka.actor.Props;
import akka.dispatch.Futures;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class DatabaseCursorMock extends StreamCursor<Iterator<Records>, Records>{
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public DatabaseCursorMock(Iterator<Records> t) {
		super(t);
	}
	
	public static Props props(Iterator<Records> t) {
		return Props.create(DatabaseCursorMock.class, t);
	}

	@Override
	protected boolean hasNext() throws Exception {
		log.debug("has next");
		
		return t.hasNext();
	}

	@Override
	protected Future<Records> next() {
		log.debug("next");
		
		return Futures.successful(t.next());
	}

}

package nl.idgis.publisher.stream;

import java.util.Iterator;

import nl.idgis.publisher.stream.messages.Item;

import scala.concurrent.Future;

import akka.actor.Props;
import akka.dispatch.Futures;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class ListCursor<T extends Item> extends StreamCursor<Iterator<T>, T>{
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public ListCursor(Iterator<T> t) {
		super(t);
	}
	
	public static <T extends Item> Props props(Iterator<T> t) {
		return Props.create(ListCursor.class, t);
	}

	@Override
	protected boolean hasNext() throws Exception {
		log.debug("has next");
		
		return t.hasNext();
	}

	@Override
	protected Future<T> next() {
		log.debug("next");
		
		return Futures.successful(t.next());
	}

}

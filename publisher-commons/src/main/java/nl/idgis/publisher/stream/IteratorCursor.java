package nl.idgis.publisher.stream;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.stream.messages.Item;

public class IteratorCursor<T extends Item> extends StreamCursor<Iterator<T>, T>{
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public IteratorCursor(Iterator<T> t) {
		super(t);
	}
	
	public static <T extends Item> Props props(Iterator<T> t) {
		return Props.create(IteratorCursor.class, t);
	}

	@Override
	protected boolean hasNext() throws Exception {
		log.debug("has next");
		
		return t.hasNext();
	}

	@Override
	protected CompletableFuture<T> next() {
		log.debug("next");
		
		return f.successful(t.next());
	}

}

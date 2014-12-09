package nl.idgis.publisher.provider.mock;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import nl.idgis.publisher.provider.protocol.metadata.MetadataItem;
import nl.idgis.publisher.stream.StreamCursor;

import scala.concurrent.Future;

import akka.actor.Props;
import akka.dispatch.Futures;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class MetadataCursorMock extends StreamCursor<Iterator<Map.Entry<String, byte[]>>, MetadataItem> {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public MetadataCursorMock(Map<String, byte[]> metadataDocuments) {
		super(metadataDocuments.entrySet().iterator());
	}
	
	public static Props props(Map<String, byte[]> metadataDocuments) {
		return Props.create(MetadataCursorMock.class, metadataDocuments);
	}

	@Override
	protected boolean hasNext() throws Exception {
		log.debug("has next");
		
		return t.hasNext();
	}

	@Override
	protected Future<MetadataItem> next() {
		log.debug("next");
		
		Entry<String, byte[]> entry = t.next();
		return Futures.successful(new MetadataItem(entry.getKey(), entry.getValue()));
	}
	
}
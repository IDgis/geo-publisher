package nl.idgis.publisher.provider.metadata;

import java.io.File;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.provider.metadata.messages.MetadataItem;
import nl.idgis.publisher.stream.StreamCursor;

public class MetadataCursor extends StreamCursor<Iterator<File>, MetadataItem>{
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public MetadataCursor(Iterator<File> t) {
		super(t);	
	}
	
	public static Props props(Iterator<File> t) {
		return Props.create(MetadataCursor.class, t);
	}

	@Override
	protected boolean hasNext() throws Exception {
		return t.hasNext();
	}

	@Override
	protected CompletableFuture<MetadataItem> next() {
		try {
			File file = t.next();
			log.debug("parsing metadata: {}", file);
			return f.successful(MetadataParser.createMetadataItem(file));
		} catch(Exception e) {
			return f.failed(e);
		}
	}
}

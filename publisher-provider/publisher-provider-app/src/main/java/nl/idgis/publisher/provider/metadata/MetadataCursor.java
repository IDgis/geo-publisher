package nl.idgis.publisher.provider.metadata;

import java.io.File;
import java.util.Iterator;

import akka.actor.Props;
import scala.concurrent.Future;
import nl.idgis.publisher.provider.protocol.metadata.MetadataItem;
import nl.idgis.publisher.stream.StreamCursor;

public class MetadataCursor extends StreamCursor<Iterator<File>, MetadataItem>{

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
	protected Future<MetadataItem> next() {
		return askActor(getContext().actorOf(MetadataParser.props()), t.next(), 1000);
	}
}

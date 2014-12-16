package nl.idgis.publisher.provider.metadata;

import java.io.File;
import java.util.Iterator;

import akka.actor.Props;
import akka.dispatch.Futures;

import scala.concurrent.Future;

import nl.idgis.publisher.provider.metadata.messages.MetadataItem;
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
		try {
			return Futures.successful(MetadataParser.createMetadataItem(t.next()));
		} catch(Exception e) {
			return Futures.failed(e);
		}
	}
}

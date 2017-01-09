package nl.idgis.publisher.provider.metadata;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.provider.metadata.messages.MetadataItem;
import nl.idgis.publisher.provider.metadata.messages.StoreFileNames;
import nl.idgis.publisher.stream.StreamCursor;

public class MetadataCursor extends StreamCursor<Iterator<File>, MetadataItem>{
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef metadata;
	
	private MetadataDocumentFactory metadataDocumentFactory;
	
	private Map<String, String> fileNames;

	public MetadataCursor(Iterator<File> t, ActorRef metadata) {
		super(t);
		
		this.metadata = metadata;
	}
	
	public static Props props(Iterator<File> t, ActorRef metadata) {
		return Props.create(MetadataCursor.class, t, metadata);
	}
	
	@Override
	protected void preStartElse() throws Exception {
		metadataDocumentFactory = new MetadataDocumentFactory();
		fileNames = new HashMap<>();
	}

	@Override
	protected boolean hasNext() throws Exception {
		boolean retval = t.hasNext();
		
		if(!retval) {
			 metadata.tell(new StoreFileNames(fileNames), getSelf());
		}
		
		return retval;
	}

	@Override
	protected CompletableFuture<MetadataItem> next() {
		try {
			File file = t.next();
			
			log.debug("parsing metadata: {}", file);
			
			MetadataItem metadataItem = MetadataParser.createMetadataItem(file);
			MetadataDocument metadataDocument = metadataDocumentFactory.parseDocument(metadataItem.getContent());
			
			String fileName = file.getName();
			String identification = metadataDocument.getDatasetIdentifier();
			
			log.debug("metadata fileName: {}, identification: {}", fileName, identification);
			fileNames.put(identification, fileName);
			
			return f.successful(metadataItem);
		} catch(Exception e) {
			return f.failed(e);
		}
	}
}

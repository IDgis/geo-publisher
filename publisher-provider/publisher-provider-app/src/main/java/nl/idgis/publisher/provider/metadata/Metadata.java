package nl.idgis.publisher.provider.metadata;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.provider.metadata.messages.GetAllMetadata;
import nl.idgis.publisher.provider.metadata.messages.GetMetadata;
import nl.idgis.publisher.provider.metadata.messages.MetadataItem;
import nl.idgis.publisher.provider.metadata.messages.MetadataNotFound;
import nl.idgis.publisher.provider.metadata.messages.StoreFileNames;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Metadata extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final File metadataDirectory;
	
	private ActorRef listProvider;
	
	private Map<String, String> fileNames;
	
	public Metadata(File metadataDirectory) {
		if (!metadataDirectory.isDirectory()) {
			throw new IllegalArgumentException("metadataDirectory is not a directory");
		}
		
		this.metadataDirectory = metadataDirectory;
	}
	
	public static Props props(File metadataDirectory) {
		return Props.create(Metadata.class, metadataDirectory);
	}
	
	@Override
	public void preStart() throws Exception {
		listProvider = getContext().actorOf(MetadataListProvider.props(metadataDirectory, getSelf()), "list");
		fileNames = new HashMap<>();
		
		log.debug("building initial fileName map");
		
		MetadataDocumentFactory metadataDocumentFactory = new MetadataDocumentFactory();
		for(File file : metadataDirectory.listFiles()) {
			if(file.isFile()) {
				try {
					MetadataItem metadataItem = MetadataParser.createMetadataItem(file);
					MetadataDocument metadataDocument = metadataDocumentFactory.parseDocument(metadataItem.getContent());
					
					String fileName = file.getName();
					String identification = metadataDocument.getDatasetIdentifier();
					
					log.debug("metadata fileName: {}, identification: {}", fileName, identification);
					fileNames.put(identification, fileName);
				} catch(Exception e) {
					try {
						log.error(e, "couldn't process metadata file: " + file.getCanonicalPath());
					} catch(IOException e2) {
						log.error(e, "couldn't process metadata file (no canonical path): " + file.getName());
					}
				}
			}
		}
		
		log.debug("fileName map created");
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof GetAllMetadata) {			
			handleGetAllMetadata((GetAllMetadata)msg);
		} else if(msg instanceof GetMetadata) {
			handleGetMetadata((GetMetadata)msg);
		} else if(msg instanceof StoreFileNames) {
			handleStoreFileNames((StoreFileNames)msg);
		} else {
			unhandled(msg);
		}
	}	

	private void handleStoreFileNames(StoreFileNames msg) {
		fileNames = msg.getFileNames();
		
		log.debug("new fileName map received: {}", fileNames);
	}

	private void handleGetMetadata(GetMetadata msg) throws IOException {
		String identification = msg.getIdentification();
		
		log.debug("fetching single metadata document: " + identification);
		
		if(fileNames.containsKey(identification)) {
			String fileName = fileNames.get(identification);
			log.debug("file name: {}", fileName);
			
			File file = new File(metadataDirectory, fileName);
			
			try {
				MetadataItem metadataItem = MetadataParser.createMetadataItem(file);
				getSender().tell(metadataItem, getSelf());
				
				log.debug("fetched");
			} catch(Exception e) {
				getSender().tell(new MetadataNotFound(identification), getSelf());
			}
		} else {
			getSender().tell(new MetadataNotFound(identification), getSelf());
		}
	}

	private void handleGetAllMetadata(GetAllMetadata msg) {
		log.debug("listing all metadata");
		listProvider.tell(msg, getSender());
	}

}

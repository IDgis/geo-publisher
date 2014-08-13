package nl.idgis.publisher.provider.metadata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.protocol.metadata.GetAllMetadata;
import nl.idgis.publisher.provider.protocol.metadata.GetMetadata;
import nl.idgis.publisher.provider.protocol.metadata.MetadataItem;
import nl.idgis.publisher.provider.protocol.metadata.PutMetadata;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Metadata extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final File metadataDirectory;
	
	private ActorRef listProvider;
	
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
		listProvider = getContext().actorOf(MetadataListProvider.props(metadataDirectory), "list");
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof GetAllMetadata) {			
			handleGetAllMetadata((GetAllMetadata)msg);
		} else if(msg instanceof GetMetadata) {
			handleGetMetadata((GetMetadata)msg);
		} else if(msg instanceof PutMetadata) {
			handlePutMetadata((PutMetadata)msg);
		} else {
			unhandled(msg);
		}
	}

	private void handlePutMetadata(PutMetadata msg) throws IOException {
		String id = msg.getIdentification();
		
		log.debug("saving single metadata document: " + id);
		File document = getFile(id);
		
		FileOutputStream fos = new FileOutputStream(document);
		IOUtils.write(msg.getContent(), fos);
		fos.close();
		
		log.debug("saved");
		
		getSender().tell(new Ack(), getSelf());
	}

	private void handleGetMetadata(GetMetadata msg) throws IOException {
		String id = msg.getIdentification();
		
		log.debug("fetching single metadata document: " + id);
		File document = getFile(id);

		MetadataItem metadataItem = MetadataParser.createMetadataItem(document);
		getSender().tell(metadataItem, getSelf());
		
		log.debug("fetched");
	}

	private File getFile(String id) {
		File document = new File(metadataDirectory, id + ".xml");
		return document;
	}

	private void handleGetAllMetadata(GetAllMetadata msg) {
		log.debug("listing all metadata");
		listProvider.tell(msg, getSender());
	}

}

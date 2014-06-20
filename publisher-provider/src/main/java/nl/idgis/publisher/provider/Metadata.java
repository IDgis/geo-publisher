package nl.idgis.publisher.provider;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;

import nl.idgis.publisher.protocol.Message;
import nl.idgis.publisher.protocol.metadata.EndOfList;
import nl.idgis.publisher.protocol.metadata.GetList;
import nl.idgis.publisher.protocol.metadata.Item;
import nl.idgis.publisher.protocol.metadata.NextItem;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Metadata extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final File metadataDirectory;
	
	private Iterator<File> fileIterator;
	
	public Metadata(File metadataDirectory) {
		if(!metadataDirectory.isDirectory()) {
			throw new IllegalArgumentException("metadataDirectory is not a directory");
		}
		
		this.metadataDirectory = metadataDirectory;
	}
	
	public static Props props(File metadataDirectory) {
		return Props.create(Metadata.class, metadataDirectory);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof GetList) {
			log.debug("metadata list requested");
			
			if(fileIterator != null) {
				throw new IllegalStateException("fileIterator != null");
			}
			
			fileIterator = Arrays.asList(metadataDirectory.listFiles()).iterator();
			nextItem();
		} else if(msg instanceof NextItem) {
			log.debug("next metadata list item requested");
			
			nextItem();
		} else {		
			unhandled(msg);
		}
	}
	
	private void nextItem() {
		if(fileIterator == null) {
			throw new IllegalStateException("fileIterator == null");
		}
		
		final Object response;
		if(fileIterator.hasNext()) {			
			response = new Item(fileIterator.next().getName());			
		} else {
			response = new EndOfList();
			fileIterator = null;
		}
		
		log.debug("sending next item response");		
		getSender().tell(new Message("harvester", response), getSelf());
	}
}

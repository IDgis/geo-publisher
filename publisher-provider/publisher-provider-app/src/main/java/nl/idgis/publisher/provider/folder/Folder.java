package nl.idgis.publisher.provider.folder;

import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import nl.idgis.publisher.provider.folder.messages.FetchFile;
import nl.idgis.publisher.provider.folder.messages.FileNotExists;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.UniqueNameGenerator;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Folder extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
	
	private final Path root;
	
	public Folder(Path root) {
		this.root = root;
	}
	
	public static Props props(Path root) {
		return Props.create(Folder.class, root);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof FetchFile) {
			handleFetchFile((FetchFile)msg);
		} else {		
			unhandled(msg);
		}
	}

	private void handleFetchFile(FetchFile msg) throws Exception {
		Path file = msg.getFile();
		
		log.debug("fetching file: {}", file);
		
		if(file.isAbsolute()) {
			log.debug("is absolute");
			
			unhandled(msg);
		} else {	
			log.debug("is relative");
			
			Path resolvedFile = root.resolve(file);			
			if(Files.exists(resolvedFile)) {
				log.debug("exists");
				
				if(Files.isDirectory(resolvedFile)) {
					log.debug("is directory");
				} else {
					log.debug("is file");
					
					ActorRef cursor = getContext().actorOf(
						ChannelCursor.props(AsynchronousFileChannel.open(
							resolvedFile, 
							StandardOpenOption.READ)),
						nameGenerator.getName(ChannelCursor.class));
					cursor.tell(new NextItem(), getSender());
				}
			} else {
				log.debug("not exists");
				
				getSender().tell(new FileNotExists(), getSelf());
			}
		}
	}	

}

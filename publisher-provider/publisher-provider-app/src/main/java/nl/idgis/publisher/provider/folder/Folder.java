package nl.idgis.publisher.provider.folder;

import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.provider.folder.messages.FetchFile;
import nl.idgis.publisher.provider.folder.messages.FileNotExists;
import nl.idgis.publisher.provider.folder.messages.FileSize;
import nl.idgis.publisher.provider.folder.messages.GetFileSize;
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
		} else if(msg instanceof GetFileSize) {
			handleGetFileSize((GetFileSize)msg);
		} else {
			unhandled(msg);
		}
	}

	private void handleGetFileSize(GetFileSize msg) {
		Path file = msg.getFile();
		
		log.debug("determine file size: {}", file);
		
		ActorRef sender = getSender();
		resolveFile(msg, file, resolvedFile -> {
			try {
				sender.tell(new FileSize(Files.size(resolvedFile)), getSelf());
			} catch(Exception e) {
				sender.tell(new Failure(e), getSelf());
			}
		});
	}
	
	private void resolveFile(Object msg, Path file, Consumer<Path> func) {
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
					
					unhandled(msg);
				} else {
					log.debug("is file");
					
					func.accept(resolvedFile);
				}
			} else {
				log.debug("not exists");
				
				getSender().tell(new FileNotExists(), getSelf());
			}
		}
	}

	private void handleFetchFile(FetchFile msg) throws Exception {
		Path file = msg.getFile();
		
		log.debug("fetching file: {}", file);
		
		ActorRef sender = getSender();
		resolveFile(msg, file, resolvedFile -> {
			try {
			ActorRef cursor = getContext().actorOf(
				ChannelCursor.props(AsynchronousFileChannel.open(
					resolvedFile, 
					StandardOpenOption.READ)),
				nameGenerator.getName(ChannelCursor.class));
			cursor.tell(new NextItem(), sender);
			} catch(Exception e) {
				sender.tell(new Failure(e), getSelf());
			}
		});
	}	

}

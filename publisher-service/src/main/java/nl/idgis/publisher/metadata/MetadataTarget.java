package nl.idgis.publisher.metadata;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import nl.idgis.publisher.metadata.messages.BeginMetadataUpdate;
import nl.idgis.publisher.metadata.messages.CommitMetadata;
import nl.idgis.publisher.metadata.messages.KeepDatasetMetadata;
import nl.idgis.publisher.metadata.messages.KeepServiceMetadata;
import nl.idgis.publisher.metadata.messages.UpdateDatasetMetadata;
import nl.idgis.publisher.metadata.messages.UpdateServiceMetadata;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;

public class MetadataTarget extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Path serviceMetadataDirectory, datasetMetadataDirectory;
		
	public MetadataTarget(Path serviceMetadataDirectory, Path datasetMetadataDirectory) {
		this.serviceMetadataDirectory = serviceMetadataDirectory;
		this.datasetMetadataDirectory = datasetMetadataDirectory;
	}
	
	private static Path requireDirectory(Path path, String notDirectoryMessage, String cannotCreateMessage) {
		if(Files.exists(path)) {
			if(!Files.isDirectory(path)) {
				throw new IllegalArgumentException(notDirectoryMessage);
			}
		} else {
			try {
				Files.createDirectories(path);
			} catch(IOException e) {
				throw new IllegalArgumentException(cannotCreateMessage, e);
			}
		}
		
		return path;
	}
	
	public static Props props(Path serviceMetadataDirectory, Path datasetMetadataDirectory) {
		return Props.create(
			MetadataTarget.class, 
			requireDirectory(
				Objects.requireNonNull(serviceMetadataDirectory, "serviceMetadataDirectory must not be null"), 
				"serviceMetadataDirectory must be a directory",
				"can not create serviceMetadataDirectory"), 
			requireDirectory(
				Objects.requireNonNull(datasetMetadataDirectory, "datasetMetadataDirectory must not be null"),
				"datasetMetadataDirectory must be a directory",
				"can not create datasetMetadataDirectory"));
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof BeginMetadataUpdate) {
			handleBeginMetadataUpdate();
		} else {
			unhandled(msg);
		}
	}
	
	private void move(Path source, Path target) throws IOException {
		log.debug("moving files, source: {}, target: {}", source, target);
		
		Files.move(source, target);
	}
	
	private void delete(Path dir) throws IOException {
		log.debug("deleting directory: {}", dir);
		
		Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
			
		});
	}
	
	private Path createTempDirectory(Path baseDirectory) {
		String name = baseDirectory.getFileName().toString();
		Path parent = baseDirectory.getParent();
		
		Path retval;
		int count = 0;
		do {
			retval = parent.resolve("_" + name + count);
			count++;
		} while(Files.exists(retval));
		
		return retval;
	}
	
	private Procedure<Object> updatingMetadata(Path serviceMetadataTempDirectory, Path datasetMetadataTempDirectory) throws Exception {		
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof UpdateServiceMetadata) {
					handleUpdateServiceMetadata((UpdateServiceMetadata)msg);
				} else if(msg instanceof UpdateDatasetMetadata) {
					handleUpdateDatasetMetadata((UpdateDatasetMetadata)msg);
				} else if(msg instanceof KeepServiceMetadata) {
					handleKeepServiceMetadata((KeepServiceMetadata)msg);
				} else if(msg instanceof KeepDatasetMetadata) {
					handleKeepDatasetMetadata((KeepDatasetMetadata)msg);				 
				} else if(msg instanceof CommitMetadata) {
					handleCommitMetadata();
				} else {
					unhandled(msg);
				}
			}
			
			private void handleUpdateDatasetMetadata(UpdateDatasetMetadata msg) {
				String datasetId = msg.getDatasetId();
				doUpdate(datasetMetadataTempDirectory, datasetId, msg.getMetadataDocument());
			}

			private void handleUpdateServiceMetadata(UpdateServiceMetadata msg) {
				String serviceId = msg.getServiceId();
				doUpdate(serviceMetadataTempDirectory, serviceId, msg.getMetadataDocument());
			}
			
			private void handleKeepDatasetMetadata(KeepDatasetMetadata msg) {
				String datasetId = msg.getDatasetId();
				doKeep(datasetMetadataDirectory, datasetMetadataTempDirectory, datasetId);
			}
			
			private void handleKeepServiceMetadata(KeepServiceMetadata msg) {
				String serviceId = msg.getServiceId();
				doKeep(serviceMetadataDirectory, serviceMetadataTempDirectory, serviceId);
			}

			private void handleCommitMetadata() {
				log.debug("moving temp directories to target directories");
				
				try {
					Path movedDatasetMetadataDirectory = createTempDirectory(datasetMetadataDirectory);
					Path movedServiceMetadataDirectory = createTempDirectory(serviceMetadataDirectory);
					
					move(datasetMetadataDirectory, movedDatasetMetadataDirectory);
					move(serviceMetadataDirectory, movedServiceMetadataDirectory);
					
					move(datasetMetadataTempDirectory, datasetMetadataDirectory);
					move(serviceMetadataTempDirectory, serviceMetadataDirectory);
					
					delete(movedDatasetMetadataDirectory);
					delete(movedServiceMetadataDirectory);
					
					getSender().tell(new Ack(), getSelf());
				} catch(Exception e) {
					Failure f = new Failure(e);
					
					log.error("couldn't commit metadata: {}", f);
					getSender().tell(f, getSelf());
				}
			}
			
		};
	}	

	private void handleBeginMetadataUpdate() throws Exception {
		try {
			log.debug("start updating metadata");
			
			Path serviceMetadataTempDirectory = createTempDirectory(serviceMetadataDirectory);
			Path datasetMetadataTempDirectory = createTempDirectory(datasetMetadataDirectory);
			
			log.debug("serviceMetadataTempDirectory: {}", serviceMetadataTempDirectory);
			log.debug("datasetMetadataTempDirectory: {}", datasetMetadataTempDirectory);
			
			Files.createDirectories(serviceMetadataTempDirectory);
			Files.createDirectories(datasetMetadataTempDirectory);
			
			getContext().become(updatingMetadata(serviceMetadataTempDirectory, datasetMetadataTempDirectory));
			getSender().tell(new Ack(), getSelf());
		} catch(Exception e) {
			getSender().tell(new Failure(e), getSelf());
		}
	}

	private void doUpdate(Path target, String id, MetadataDocument metadataDocument) {
		try {
			Path file = target.resolve(getFile(id));			
			log.debug("writing metadata document to file: {}", file);
			
			Files.write(				
				file,
				metadataDocument.getContent());
			getSender().tell(new Ack(), getSelf());
		} catch(Exception e) {
			Failure failure = new Failure(e);			
			log.error("couldn't perform update: {}", failure);			
			getSender().tell(failure, getSelf());
		}
	}	
	
	private void doKeep(Path source, Path target, String id) {
		try {
			String fileName = getFile(id);
			log.debug("keeping metadata document file: {}", fileName);
		
			Files.copy(source.resolve(fileName), target.resolve(fileName));
			getSender().tell(new Ack(), getSelf());
		} catch(Exception e) {
			Failure failure = new Failure(e);			
			log.error("couldn't perform keep: {}", failure);
			getSender().tell(failure, getSelf());
		}
	}

	private String getFile(String id) throws IOException {
		return id + ".xml";
	}
}

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

import nl.idgis.publisher.metadata.messages.BeginPutMetadata;
import nl.idgis.publisher.metadata.messages.CommitMetadata;
import nl.idgis.publisher.metadata.messages.PutDatasetMetadata;
import nl.idgis.publisher.metadata.messages.PutServiceMetadata;
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
		if(msg instanceof BeginPutMetadata) {
			handleBeginPutMetadata();
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
	
	private Procedure<Object> puttingMetadata() throws Exception {
		
		Path serviceMetadataDirectory = createTempDirectory(MetadataTarget.this.serviceMetadataDirectory);
		Path datasetMetadataDirectory = createTempDirectory(MetadataTarget.this.datasetMetadataDirectory);
		
		Files.createDirectories(serviceMetadataDirectory);
		Files.createDirectories(datasetMetadataDirectory);
		
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof PutServiceMetadata) {
					handlePutServiceMetadata((PutServiceMetadata)msg);
				} else if(msg instanceof PutDatasetMetadata) {
					handlePutDatasetMetadata((PutDatasetMetadata)msg);
				} else if(msg instanceof CommitMetadata) {
					handleCommitMetadata();
				} else {
					unhandled(msg);
				}
			}
			
			private void handlePutDatasetMetadata(PutDatasetMetadata msg) {
				String datasetId = msg.getDatasetId();
				log.debug("storing dataset metadata: {}", datasetId);		
				doPut(datasetMetadataDirectory, datasetId, msg.getMetadataDocument());
			}

			private void handlePutServiceMetadata(PutServiceMetadata msg) {
				String serviceId = msg.getServiceId();
				log.debug("storing service metadata: {}", serviceId);		
				doPut(serviceMetadataDirectory, serviceId, msg.getMetadataDocument());
			}

			private void handleCommitMetadata() {
				log.debug("moving metadata to target directories");
				
				try {
					Path currentDatasetMetadataDirectory = createTempDirectory(MetadataTarget.this.datasetMetadataDirectory);
					Path currentServiceMetadataDirectory = createTempDirectory(MetadataTarget.this.serviceMetadataDirectory);
					
					move(MetadataTarget.this.datasetMetadataDirectory, currentDatasetMetadataDirectory);
					move(MetadataTarget.this.serviceMetadataDirectory, currentServiceMetadataDirectory);
					
					move(datasetMetadataDirectory, MetadataTarget.this.datasetMetadataDirectory);
					move(serviceMetadataDirectory, MetadataTarget.this.serviceMetadataDirectory);
					
					delete(currentDatasetMetadataDirectory);
					delete(currentServiceMetadataDirectory);
					
					getSender().tell(new Ack(), getSelf());
				} catch(Exception e) {
					Failure f = new Failure(e);
					
					log.error("couldn't commit metadata: {}", f);
					getSender().tell(f, getSelf());
				}
			}
			
		};
	}

	private void handleBeginPutMetadata() throws Exception {
		try {
			getContext().become(puttingMetadata());
			getSender().tell(new Ack(), getSelf());
		} catch(Exception e) {
			getSender().tell(new Failure(e), getSelf());
		}
	}

	private void doPut(Path metadataDirectory, String name, MetadataDocument metadataDocument) {
		try {
			Path file = metadataDirectory.resolve(name + ".xml");
			
			log.debug("writing to file: {}", file);
			
			Files.write(				
				file,
				metadataDocument.getContent());
			getSender().tell(new Ack(), getSelf());
		} catch(Exception e) {
			getSender().tell(new Failure(e), getSelf());
		}
	}	

}

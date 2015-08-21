package nl.idgis.publisher.metadata;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Supplier;

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
	
	private final Supplier<Path> tempDirectorySupplier;
	
	public MetadataTarget(Path serviceMetadataDirectory, Path datasetMetadataDirectory, Supplier<Path> tempDirectorySupplier) {
		this.serviceMetadataDirectory = serviceMetadataDirectory;
		this.datasetMetadataDirectory = datasetMetadataDirectory;
		this.tempDirectorySupplier = tempDirectorySupplier;
	}
	
	public static Props props(Path serviceMetadataDirectory, Path datasetMetadataDirectory) {
		return props(serviceMetadataDirectory, datasetMetadataDirectory, () -> {
			try {
				return Files.createTempDirectory("metadata");
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		});
	}
	
	public static Props props(Path serviceMetadataDirectory, Path datasetMetadataDirectory, Supplier<Path> tempDirectorySupplier) {
		return Props.create(MetadataTarget.class, serviceMetadataDirectory, datasetMetadataDirectory, tempDirectorySupplier);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof BeginPutMetadata) {
			handleBeginPutMetadata();
		} else {
			unhandled(msg);
		}
	}
	
	private Procedure<Object> puttingMetadata(Path beginTempDirectory) throws Exception {
		
		Path serviceMetadataDirectory = beginTempDirectory.resolve("service");
		Path datasetMetadataDirectory = beginTempDirectory.resolve("dataset");
		
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
				try {
					Path commitTempDirectory = tempDirectorySupplier.get();
					
					Files.createDirectories(commitTempDirectory);
					
					Files.move(MetadataTarget.this.datasetMetadataDirectory, commitTempDirectory.resolve("dataset"));
					Files.move(MetadataTarget.this.serviceMetadataDirectory, commitTempDirectory.resolve("service"));
					
					Files.move(datasetMetadataDirectory, MetadataTarget.this.datasetMetadataDirectory);
					Files.move(serviceMetadataDirectory, MetadataTarget.this.serviceMetadataDirectory);
					
					Files.delete(beginTempDirectory);
					
					Files.walkFileTree(commitTempDirectory, new SimpleFileVisitor<Path>() {

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
					
					getSender().tell(new Ack(), getSelf());
				} catch(Exception e) {
					getSender().tell(new Failure(e), getSelf());
				}
			}
			
		};
	}

	private void handleBeginPutMetadata() throws Exception {
		try {
			Path tempDirectory = tempDirectorySupplier.get();
			
			getContext().become(puttingMetadata(tempDirectory));
			getSender().tell(new Ack(), getSelf());
		} catch(Exception e) {
			getSender().tell(new Failure(e), getSelf());
		}
	}

	private void doPut(Path metadataDirectory, String name, MetadataDocument metadataDocument) {
		try {
			Files.write(				
				metadataDirectory.resolve(name + ".xml"),
				metadataDocument.getContent());
			getSender().tell(new Ack(), getSelf());
		} catch(Exception e) {
			getSender().tell(new Failure(e), getSelf());
		}
	}	

}

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
import nl.idgis.publisher.metadata.messages.KeepMetadata;
import nl.idgis.publisher.metadata.messages.MetadataType;
import nl.idgis.publisher.metadata.messages.UpdateMetadata;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;

/**
 * This actor is responsible for saving {@link MetadataDocument} objects to disk.
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
public class MetadataTarget extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Path serviceMetadataDirectory, datasetMetadataDirectory;
		
	public MetadataTarget(Path serviceMetadataDirectory, Path datasetMetadataDirectory) {
		this.serviceMetadataDirectory = serviceMetadataDirectory;
		this.datasetMetadataDirectory = datasetMetadataDirectory;
	}
	
	/**
	 * Ensures that a given {@link Path} points to an already existing directory or that a directory
	 * can be created at specified location.  
	 * 
	 * @param path the path to test.
	 * @param notDirectoryMessage the exception message used when path exists but is not a directory.
	 * @param cannotCreateMessage the exception message used when directory creation fails.
	 * @return
	 */
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
	
	/**
	 * Creates a {@link Props} for the {@link MetadataTarget} actor.
	 * 
	 * @param serviceMetadataDirectory a path pointing to the directory containing service metadata.
	 * @param datasetMetadataDirectory a path pointing to the directory containing dataset metadata.
	 * @return the props.
	 */
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

	/**
	 * Default behavior. Handles only {@link BeginMetadataUpdate}.
	 * @param msg the received message.
	 */
	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof BeginMetadataUpdate) {
			handleBeginMetadataUpdate();
		} else {
			unhandled(msg);
		}
	}
	
	/**
	 * Moves a directory. Expects to be able to move non-empty directories.
	 * This usually implies that source and target directories have to be
	 * part of the same volume / filesystem.
	 * 
	 * @param source the directory to move from.
	 * @param target the directory to move to.
	 * @throws IOException
	 */
	private void move(Path source, Path target) throws IOException {
		log.debug("moving files, source: {}, target: {}", source, target);
		
		Files.move(source, target);
	}
	
	/**
	 * Deletes a directory along with all its content. 
	 * @param directory the directory to delete.
	 * @throws IOException
	 */
	private void delete(Path directory) throws IOException {
		log.debug("deleting directory: {}", directory);
		
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {

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
	
	/**
	 * Creates a temp directory alongside a given base directory. It creates 
	 * a new directory in the parent directory of the base directory with a name
	 * starting with an underscore ('_') and containing the file name of the 
	 * base directory.
	 * 
	 * @param baseDirectory the base directory to create a temp directory for.
	 * @return the temp directory.
	 */
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
	
	/**
	 * Provides updating metadata behavior.
	 * 
	 * @param serviceMetadataTempDirectory a path pointing to the temp directory containing service metadata.
	 * @param datasetMetadataTempDirectory a path pointing to the temp directory containing dataset metadata.
	 * @return the behavior.
	 * @throws Exception
	 */
	private Procedure<Object> updatingMetadata(Path serviceMetadataTempDirectory, Path datasetMetadataTempDirectory) throws Exception {		
		return new Procedure<Object>() {

			/**
			 * Update and keep behavior. Handles {@link UpdateServiceMetadata},
			 * {@link UpdateDatasetMetadata}, {@link KeepServiceMetadata},
			 * {@link KeepDatasetMetadata} and {@link CommitMetadata}.
			 * @param msg the received message.
			 */
			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof UpdateMetadata) {
					handleUpdateMetadata((UpdateMetadata)msg);				
				} else if(msg instanceof KeepMetadata) {
					handleKeepMetadata((KeepMetadata)msg); 				 
				} else if(msg instanceof CommitMetadata) {
					handleCommitMetadata();
				} else {
					unhandled(msg);
				}
			}
			
			/**
			 * Keep metadata.
			 * @param msg the received message.
			 */
			private void handleKeepMetadata(KeepMetadata msg) {				
				MetadataType type = msg.getType();
				doKeep(getSourcePath(type), getTargetPath(type), msg.getId());
				
			}

			/**
			 * Update metadata.
			 * @param msg the received message.
			 */
			private void handleUpdateMetadata(UpdateMetadata msg) {
				doUpdate(getTargetPath(msg.getType()), msg.getId(), msg.getMetadataDocument());
			}
			
			/**
			 * Get path based on metadata type.
			 * 
			 * @param metadataType the type.
			 * @param servicePath the service path.
			 * @param metadataPath the metadata path.
			 * @return the path.
			 */
			private Path getPath(MetadataType metadataType, Path servicePath, Path metadataPath) {
				switch(metadataType) {
					case DATASET:
						return metadataPath;						
					case SERVICE:
						return servicePath;
					default:
						throw new IllegalArgumentException("unsupported metadata type: " + metadataType.name());
				}
			}
			
			/**
			 * Get the source path for a specific metadata type.
			 * 
			 * @param metadataType the metadata type.
			 * @return the source path.
			 */
			private Path getSourcePath(MetadataType metadataType) {				
				return getPath(metadataType, serviceMetadataDirectory, datasetMetadataDirectory);
			}

			/**
			 * Get the target path for a specific metadata type.
			 * 
			 * @param metadataType the metadata type.
			 * @return the target path.
			 */
			private Path getTargetPath(MetadataType metadataType) {				
				return getPath(metadataType, serviceMetadataTempDirectory, datasetMetadataTempDirectory);
			}

			/**
			 * Commit metadata by moving temp directories to target directory.
			 */
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
					
					getContext().become(receive());
				} catch(Exception e) {
					Failure f = new Failure(e);
					
					log.error("couldn't commit metadata: {}", f);
					getSender().tell(f, getSelf());
				}
			}
			
		};
	}	

	/**
	 * Starts a metadata update session by creating empty target directories.
	 * 
	 * @throws Exception
	 */
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

	/**
	 * Update document by serializing given {@link MetadataDocument} to the temp directory.
	 * 
	 * @param target the temp directory containing the updated documents. 
	 * @param id the document id.
	 * @param metadataDocument the metadata document to serialize.
	 */
	private void doUpdate(Path target, String id, MetadataDocument metadataDocument) {
		try {
			Path file = target.resolve(getFileName(id));			
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
	
	/**
	 * Keep existing document by copying the current document to the temp directory.
	 * 
	 * @param source the directory containing the current documents. 
	 * @param target the temp directory containing the updated documents.
	 * @param id the id of the document.
	 */
	private void doKeep(Path source, Path target, String id) {
		try {
			String fileName = getFileName(id);
			log.debug("keeping metadata document file: {}", fileName);
			
			Path sourceFile = source.resolve(fileName); 
			if(Files.exists(sourceFile)) {
				Files.copy(sourceFile, target.resolve(fileName));
			} else {
				log.warning("metadata document does not exist: " + sourceFile);
			}
			
			getSender().tell(new Ack(), getSelf());
		} catch(Exception e) {
			Failure failure = new Failure(e);			
			log.error("couldn't perform keep: {}", failure);
			getSender().tell(failure, getSelf());
		}
	}

	/**
	 * Get file name for given document id.
	 * 
	 * @param id the id of the document.
	 * @return the file name.
	 * @throws IOException
	 */
	private String getFileName(String id) throws IOException {
		return id + ".xml";
	}
}

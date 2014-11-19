package nl.idgis.publisher.metadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.commons.io.IOUtils;

import akka.actor.ActorRef;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.pattern.Patterns;

import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

import nl.idgis.publisher.metadata.messages.ParseMetadataDocument;

public class FileMetadataStore implements MetadataStore {
	
	private final File directory;
	
	private final ActorRef documentFactory;
	
	public FileMetadataStore(File directory, ActorRef documentFactory) {
		this.directory = directory;
		this.documentFactory = documentFactory;
	}
	
	private void deleteAll(File directory) {
		for(File f : directory.listFiles()) {
			if(f.isDirectory()) {
				deleteAll(f);				
			} 
			
			f.delete();			
		}
	}

	@Override
	public Future<Void> deleteAll() {
		deleteAll(directory);
		
		return Futures.successful(null);
	}
	
	private File getFile(String name) {
		return new File(directory, name + ".xml");
	}

	@Override
	public Future<Void> put(String name, MetadataDocument metadataDocument, ExecutionContext executionContext) {
		try {
			FileOutputStream fos = new FileOutputStream(getFile(name));
			fos.write(metadataDocument.getContent());
			fos.close();
		
			return Futures.successful(null);
		} catch(Exception e) {
			return Futures.failed(e);
		}
	}

	@Override
	public Future<MetadataDocument> get(String name, ExecutionContext executionContext) {
		try {
			File f = getFile(name);
			FileInputStream fis = new FileInputStream(f);
			
			byte[] content = new byte[(int) f.length()];
			IOUtils.readFully(fis, content);
			
			fis.close();
			
			return Patterns.ask(documentFactory, new ParseMetadataDocument(content), 15000)
					.map(new Mapper<Object, MetadataDocument>() {
						
						public MetadataDocument apply(Object msg) {
							if(msg instanceof MetadataDocument) {
								return (MetadataDocument)msg;
							} else {
								throw new IllegalArgumentException("MetadataDocument expected");
							}
						}
					}, executionContext);
		} catch(Exception e) {
			return Futures.failed(e);
		}
	}

}

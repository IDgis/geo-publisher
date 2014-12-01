package nl.idgis.publisher.metadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.commons.io.IOUtils;

import akka.dispatch.Futures;

import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

public class FileMetadataStore implements MetadataStore {
	
	private final File directory;
	
	private final MetadataDocumentFactory documentFactory;
	
	public FileMetadataStore(File directory) throws Exception {
		this.directory = directory;
		
		documentFactory = new MetadataDocumentFactory();		
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
			
			return Futures.successful(documentFactory.parseDocument(content));
		} catch(Exception e) {
			return Futures.failed(e);
		}
	}

}

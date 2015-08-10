package nl.idgis.publisher.metadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.IOUtils;

import nl.idgis.publisher.utils.FutureUtils;

public class FileMetadataStore implements MetadataStore {
	
	private final File directory;
	
	private final MetadataDocumentFactory documentFactory;
	
	private final FutureUtils f;
	
	public FileMetadataStore(File directory, FutureUtils f) throws Exception {
		this.directory = directory;
		this.f = f;
		
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
	public CompletableFuture<Void> deleteAll() {
		deleteAll(directory);
		
		return f.successful(null);
	}
	
	private File getFile(String name) {
		return new File(directory, name + ".xml");
	}

	@Override
	public CompletableFuture<Void> put(String name, MetadataDocument metadataDocument) {
		try {
			FileOutputStream fos = new FileOutputStream(getFile(name));
			fos.write(metadataDocument.getContent());
			fos.close();
		
			return f.successful(null);
		} catch(Exception e) {
			return f.failed(e);
		}
	}

	@Override
	public CompletableFuture<MetadataDocument> get(String name) {
		try {
			File file = getFile(name);
			FileInputStream fis = new FileInputStream(file);
			
			byte[] content = new byte[(int) file.length()];
			IOUtils.readFully(fis, content);
			
			fis.close();
			
			return f.successful(documentFactory.parseDocument(content));
		} catch(Exception e) {
			return f.failed(e);
		}
	}

}

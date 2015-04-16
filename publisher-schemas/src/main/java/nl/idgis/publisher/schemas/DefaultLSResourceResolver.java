package nl.idgis.publisher.schemas;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

public class DefaultLSResourceResolver implements LSResourceResolver {

	@Override
	public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
		
		try {
			InputStream inputStream = new URL(new URL(baseURI), systemId).openStream();
			
			LSInput retval = new DefaultLSInput();
			retval.setByteStream(inputStream);
			retval.setBaseURI(baseURI);
			retval.setSystemId(systemId);
			retval.setPublicId(publicId);
			
			return retval;
		} catch(IOException e) {
			return null;
		}
	}
}

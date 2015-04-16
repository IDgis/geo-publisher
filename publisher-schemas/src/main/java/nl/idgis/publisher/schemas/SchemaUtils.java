package nl.idgis.publisher.schemas;

import java.io.IOException;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

public class SchemaUtils {
	
	public static Schema getSchema(SchemaRef schema) throws SAXException, IOException {
		return getSchema(schema.getPath());
	}

	protected static Schema getSchema(String path) throws SAXException, IOException {
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schemaFactory.setResourceResolver(new DefaultLSResourceResolver());
		
		URL resource = SchemaUtils.class.getResource(path);
		return schemaFactory.newSchema(new StreamSource(resource.openStream(), resource.toExternalForm()));
	}
}

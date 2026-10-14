package util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import nl.idgis.publisher.utils.XslTransformer;

public class Transform {

	public static String transformToHtml(String base, String stylesheetUrl, byte[] content) throws IOException, TransformerException {
		String absoluteXslUrl = new URL(new URL(base), stylesheetUrl).toString();
		return XslTransformer.transform(
				new StreamSource(
						new ByteArrayInputStream(content)), absoluteXslUrl);
	}
}

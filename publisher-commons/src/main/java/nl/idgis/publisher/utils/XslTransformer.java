package nl.idgis.publisher.utils;

import javax.xml.XMLConstants;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;

public class XslTransformer {
	private static TransformerFactory createFactory() {
		TransformerFactory f = TransformerFactory.newInstance();
		try {
			f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		} catch (TransformerConfigurationException e) {
			throw new IllegalStateException(e);
		}
		
		// Stylesheets and their includes may come from these protocols:
		f.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
		// Don't let the XML load external DTDs (XXE protection):
		f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		return f;
	}

	public static String transform(Source xml, String xslUrl) throws TransformerException {
		Transformer t = createFactory().newTransformer(new StreamSource(xslUrl));
		t.setOutputProperty(OutputKeys.METHOD, "html");
		t.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "about:legacy-compat");
		StringWriter out = new StringWriter();
		t.transform(xml, new StreamResult(out));
		return out.toString();
	}

	public static String transform(String xml, String xslUrl) throws TransformerException {
		return transform(new StreamSource(new StringReader(xml)), xslUrl);
	}
}

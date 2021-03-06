package controllers;

import static models.Domain.from;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

import actions.DefaultAuthenticator;
import akka.actor.ActorSelection;

import nl.idgis.publisher.domain.query.GetMetadata;

import play.Play;
import play.libs.Akka;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

@Security.Authenticated (DefaultAuthenticator.class)
public class Metadata extends Controller {

	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");
	
	private final static String datasetMetadata = Play.application().configuration().getString("publisher.metadata.dataset");
	
	private final static String serviceMetadata = Play.application().configuration().getString("publisher.metadata.service");
	
	private final static String datasetStylesheet;
	
	static {
		String stylesheetPrefix = Play.application().configuration().getString("publisher.metadata.stylesheet");
		if(stylesheetPrefix == null) {
			datasetStylesheet = null;
		} else {
			datasetStylesheet =  stylesheetPrefix + "datasets/intern/metadata.xsl";
		}
	}
	
	public static Promise<Result> sourceDataset(final String sourceDatasetId) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from(database)
			.query(new GetMetadata(sourceDatasetId))
			.execute(metadata -> {
				if(metadata == null) {
					return notFound();
				}
				
				String stylesheet = null;
				Boolean noStyle = Boolean.parseBoolean(request().getQueryString("noStyle"));
				if(!noStyle) {
					stylesheet = datasetStylesheet;
				}
				
				return ok(setStylesheet(metadata.content(), stylesheet)).as("application/xml");
			});
	}
	
	private static byte[] setStylesheet(byte[] origContent, String stylesheet) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document d = db.parse(new ByteArrayInputStream(origContent));
		
		// remove existing stylesheet (if any)
		NodeList children = d.getChildNodes();
		for(int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if(n.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
				ProcessingInstruction pi = (ProcessingInstruction)n;
				if("xml-stylesheet".equals(pi.getTarget())) {
					d.removeChild(pi);
				}
			}
		}
		
		if(stylesheet != null) {
			// add stylesheet
			d.insertBefore(
					d.createProcessingInstruction(
						"xml-stylesheet", 
						"type=\"text/xsl\" href=\"" + stylesheet + "\""),
					d.getFirstChild());
		}
		
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = tf.newTransformer();
		
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		t.transform(new DOMSource(d), new StreamResult(boas));
		boas.close();
		
		return boas.toByteArray();
	}
}

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

import akka.actor.ActorSelection;
import nl.idgis.publisher.domain.query.GetMetadata;
import play.Play;
import play.libs.Akka;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.mvc.Controller;
import play.mvc.Result;

import static models.Domain.from;

public class Metadata extends Controller {

	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");
	
	private final static String datasetMetadata = Play.application().configuration().getString("publisher.metadata.dataset");
	
	private final static String serviceMetadata = Play.application().configuration().getString("publisher.metadata.service");
	
	private static Promise<Result> getDocument(String url) {
		return WS.url(url).get().map(response -> {
			if(response.getStatus() == 200) {			
				return ok(removeStylesheet(response.asByteArray())).as("application/xml");
			} else {
				return internalServerError();
			}
		});
	}
	
	public static Promise<Result> dataset(final String fileId) {
		return getDocument(datasetMetadata + fileId + ".xml");
	}
	
	public static Promise<Result> service(final String fileId) {
		return getDocument(serviceMetadata + fileId + ".xml");
	}
	
	public static Promise<Result> sourceDataset(final String sourceDatasetId) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from(database)
			.query(new GetMetadata(sourceDatasetId))
			.execute(metadata -> {
				if(metadata == null) {
					return notFound();
				}
				
				return ok(removeStylesheet(metadata.content())).as("application/xml");
			});
	}
	
	private static byte[] removeStylesheet(byte[] origContent) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document d = db.parse(new ByteArrayInputStream(origContent));
		
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
		
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = tf.newTransformer();
		
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		t.transform(new DOMSource(d), new StreamResult(boas));
		boas.close();
		
		return boas.toByteArray();
	}
}

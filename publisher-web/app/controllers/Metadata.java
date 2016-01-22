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
import play.mvc.Controller;
import play.mvc.Result;

import static models.Domain.from;

public class Metadata extends Controller {

	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");
	
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

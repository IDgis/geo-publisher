package controllers;

import static models.Domain.from;

import java.io.ByteArrayInputStream;

import java.net.URL;
import javax.xml.transform.stream.StreamSource;

import actions.DefaultAuthenticator;
import akka.actor.ActorSelection;

import nl.idgis.publisher.domain.query.GetMetadata;
import nl.idgis.publisher.utils.XslTransformer;

import play.Play;
import play.libs.Akka;
import play.libs.F.Promise;
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
		datasetStylesheet =  stylesheetPrefix + "datasets/intern/metadata.xsl";
	}
	
	public static Promise<Result> sourceDataset(final String sourceDatasetId) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from(database)
			.query(new GetMetadata(sourceDatasetId))
			.execute(metadata -> {
				if(metadata == null) {
					return notFound();
				}
				
				String base = routes.Metadata.sourceDataset(sourceDatasetId).absoluteURL(request());
				String absoluteXslUrl = new URL(new URL(base), datasetStylesheet).toString();
				String html = XslTransformer.transform(
						new StreamSource(
								new ByteArrayInputStream(metadata.content())), absoluteXslUrl);
				
				return ok(html).as("text/html; charset=utf-8");
			});
	}
}

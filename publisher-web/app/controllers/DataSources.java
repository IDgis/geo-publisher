package controllers;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.DataSource;
import models.Domain;
import akka.actor.ActorSelection;
import play.Play;
import play.libs.Akka;
import play.libs.F.Promise;
import play.libs.F.Function;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.datasources.list;

public class DataSources extends Controller {

	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");
	
	public static Promise<Result> list () {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return Domain
			.from (database)
			.list (DataSource.class)
			.execute (new Function<Page<DataSource>, Result> () {
				@Override
				public Result apply (final Page<DataSource> page) throws Throwable {
					return ok (list.render (page.values ()));
				}
			});
	}
}

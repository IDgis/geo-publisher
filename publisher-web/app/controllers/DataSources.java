package controllers;

import models.Domain;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.DataSource;
import play.Play;
import play.libs.Akka;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.datasources.list;
import actors.Database;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;

public class DataSources extends Controller {

	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");
	private static ActorRef databaseActor = Akka.system().actorOf (Props.create (Database.class), "database");
	
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

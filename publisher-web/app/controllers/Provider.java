package controllers;

import static models.Domain.from;

import com.google.gson.Gson;

import akka.actor.ActorSelection;
import models.Domain.Function;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.DataSource;
import play.Play;
import play.libs.Akka;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;

public class Provider extends Controller {
	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");
	
	public static Promise<Result> connection() {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from (database)
			.list (DataSource.class)
			.execute (new Function<Page<DataSource>, Result> () {
				@Override
				public Result apply (final Page<DataSource> dataSources) throws Throwable {
					String json = new Gson().toJson(dataSources.values());
					
					return ok(json).as("application/json; charset=utf-8");
				}
			});
		
	}
}

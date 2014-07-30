package controllers;

import static models.Domain.from;
import models.Domain.Function;
import models.Domain.Function2;
import nl.idgis.publisher.domain.query.ListDatasets;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.DataSource;
import nl.idgis.publisher.domain.web.Dataset;
import nl.idgis.publisher.domain.web.SourceDatasetStats;
import actions.DefaultAuthenticator;
import actors.Database;
import akka.actor.ActorSelection;
import play.Play;
import play.libs.Akka;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.datasets.list;
import views.html.datasets.form;

@Security.Authenticated (DefaultAuthenticator.class)
public class Datasets extends Controller {
	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");

	public static Promise<Result> list () {
		return listByCategoryAndMessages(null, false);
	}
	
	
	public static Promise<Result> listWithMessages () {
		return listByCategoryAndMessages(null, true);
	}
	
	public static Promise<Result> listByCategory (String categoryId) {
		return listByCategoryAndMessages(categoryId, false);
	}
	
	
	public static Promise<Result> listByCategoryWithMessages (String categoryId) {
		return listByCategoryAndMessages(categoryId, true);
	}
	
	public static Result createForm () {
		return ok (form.render ());
	}
	
	
	public static Promise<Result> listByCategoryAndMessages (final String categoryId, final boolean listWithMessages) {
		// Hack: force the database actor to be loaded:
		if (Database.instance == null) {
			throw new NullPointerException ();
		}
		
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from (database)
			.list (Category.class)
			.get (Category.class, categoryId)
			.executeFlat (new Function2<Page<Category>, Category, Promise<Result>> () {
				@Override
				public Promise<Result> apply (final Page<Category> categories, final Category currentCategory) throws Throwable {
					
					return from (database)
							.query (new ListDatasets (currentCategory))
							.execute (new Function<Page<Dataset>, Result> () {
								@Override
								public Result apply (final Page<Dataset> datasets) throws Throwable {
									
//									return ok (list.render (listWithMessages));
									return ok (list.render (datasets, categories.values (), currentCategory, listWithMessages));
								}
								
							});
				}
			});
	}

}

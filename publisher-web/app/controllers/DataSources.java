package controllers;

import java.util.ArrayList;

import models.Domain;
import models.Domain.Function4;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.DataSource;
import nl.idgis.publisher.domain.web.SourceDataset;
import play.Play;
import play.libs.Akka;
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
		return listByDataSourceAndCategory (null, null);
	}
	
	public static Promise<Result> listByDataSource (final String dataSourceId) {
		return listByDataSourceAndCategory (dataSourceId, null);
	}
	
	public static Promise<Result> listByCategory (final String categoryId) {
		return listByDataSourceAndCategory (null, categoryId);
	}
	
	public static Promise<Result> listByDataSourceAndCategory (final String dataSourceId, final String categoryId) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return Domain
			.from (database)
			.list (DataSource.class)
			.list (Category.class)
			.get (DataSource.class, dataSourceId)
			.get (Category.class, categoryId)
			.execute (new Function4<Page<DataSource>, Page<Category>, DataSource, Category, Result> () {
				@Override
				public Result apply (final Page<DataSource> dataSources, final Page<Category> categories, final DataSource currentDataSource, final Category currentCategory) throws Throwable {
					return ok (list.render (new ArrayList<SourceDataset> (), dataSources.values (), categories.values (), currentDataSource, currentCategory));
				}
			});
	}
}

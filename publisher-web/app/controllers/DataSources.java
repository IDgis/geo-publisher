package controllers;

import static models.Domain.from;
import models.Domain.Function;
import models.Domain.Function4;
import nl.idgis.publisher.domain.query.ListSourceDatasets;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.DataSource;
import nl.idgis.publisher.domain.web.SourceDatasetStats;
import play.Play;
import play.libs.Akka;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.datasources.list;
import actions.DefaultAuthenticator;
import actors.Database;
import akka.actor.ActorSelection;

@Security.Authenticated (DefaultAuthenticator.class)
public class DataSources extends Controller {
	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");

	public static Promise<Result> list (final long page) {
		return listByDataSourceAndCategory (null, null, page);
	}
	
	public static Promise<Result> listByDataSource (final String dataSourceId, final long page) {
		return listByDataSourceAndCategory (dataSourceId, null, page);
	}
	
	public static Promise<Result> listByCategory (final String categoryId, final long page) {
		return listByDataSourceAndCategory (null, categoryId, page);
	}
	
	public static Promise<Result> listByDataSourceAndCategory (final String dataSourceId, final String categoryId, final long page) {
			return listByDataSourceAndCategoryAndSearchString (dataSourceId, categoryId, null, page);
	}
	
	public static Promise<Result> search (final String search, final long page) {
		return listByDataSourceAndCategoryAndSearchString (null, null, search, page);
	}
	
	private static Promise<Result> listByDataSourceAndCategoryAndSearchString (final String dataSourceId, final String categoryId, final String search, final long page) {
		// Hack: force the database actor to be loaded:
		if (Database.instance == null) {
			throw new NullPointerException ();
		}
		
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from (database)
			.list (DataSource.class)
			.list (Category.class)
			.get (DataSource.class, dataSourceId)
			.get (Category.class, categoryId)
			.executeFlat (new Function4<Page<DataSource>, Page<Category>, DataSource, Category, Promise<Result>> () {
				@Override
				public Promise<Result> apply (final Page<DataSource> dataSources, final Page<Category> categories, final DataSource currentDataSource, final Category currentCategory) throws Throwable {
					
					return from (database)
							.query (new ListSourceDatasets (currentDataSource, currentCategory, search, page))
							.execute (new Function<Page<SourceDatasetStats>, Result> () {
								@Override
								public Result apply (final Page<SourceDatasetStats> sourceDatasets) throws Throwable {
									
									return ok (list.render (sourceDatasets, dataSources.values (), categories.values (), currentDataSource, currentCategory));
								}
								
							});
				}
			});
	}
}

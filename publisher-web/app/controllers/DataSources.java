package controllers;

import static models.Domain.from;

import java.util.ArrayList;
import java.util.List;

import play.Play;
import play.libs.Akka;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

import com.fasterxml.jackson.databind.node.ObjectNode;

import views.html.datasources.list;
import actions.DefaultAuthenticator;

import akka.actor.ActorSelection;

import models.Domain.Function;
import models.Domain.Function4;

import nl.idgis.publisher.domain.query.ListSourceDatasets;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.DataSource;
import nl.idgis.publisher.domain.web.SourceDataset;
import nl.idgis.publisher.domain.web.SourceDatasetStats;

@Security.Authenticated (DefaultAuthenticator.class)
public class DataSources extends Controller {	
	
	private static ActorSelection adminActor () {
		return Akka.system().actorSelection (
			Play.application().configuration().getString("publisher.database.actorRef")
			+ "/data-sources");
	}

	public static Promise<Result> list (final String search, final long page) {
		return listByDataSourceAndCategory (null, null, search, page);
	}
	
	public static Promise<Result> listByDataSource (final String dataSourceId, final String search, final long page) {
		return listByDataSourceAndCategory (dataSourceId, null, search, page);
	}
	
	public static Promise<Result> listByCategory (final String categoryId, final String search, final long page) {
		return listByDataSourceAndCategory (null, categoryId, search, page);
	}
	
	public static Promise<Result> listByDataSourceAndCategoryJson (final String dataSourceId, final String categoryId) {
		return from(adminActor ())
			.query(new ListSourceDatasets (dataSourceId, categoryId))
			.execute(new Function<Page<SourceDatasetStats>, Result>() {

				@Override
				public Result apply(Page<SourceDatasetStats> sourceDatasetsStats) throws Throwable {
					List<ObjectNode> jsonSourceDatasets = new ArrayList<ObjectNode>();
					
					for(SourceDatasetStats sourceDatasetStats : sourceDatasetsStats.values()) {
						SourceDataset sourceDataset = sourceDatasetStats.sourceDataset();
						
						ObjectNode jsonSourceDataset = Json.newObject();
						jsonSourceDataset.put("id", sourceDataset.id());
						jsonSourceDataset.put("name", sourceDataset.name());
						jsonSourceDataset.put("count", sourceDatasetStats.datasetCount());						
						
						jsonSourceDatasets.add(jsonSourceDataset);
					}
					
					ObjectNode result = Json.newObject();
					result.put("sourceDatasets", Json.toJson(jsonSourceDatasets));					
					
					return ok(result);
				}				
			});
	}
	
	public static Promise<Result> listByDataSourceAndCategory (final String dataSourceId, final String categoryId, final String search, final long page) {
			return listByDataSourceAndCategoryAndSearchString (dataSourceId, categoryId, search, page);
	}
	
	public static Promise<Result> search (final String search, final long page) {
		return listByDataSourceAndCategoryAndSearchString (null, null, search, page);
	}
	
	private static Promise<Result> listByDataSourceAndCategoryAndSearchString (final String dataSourceId, final String categoryId, final String search, final long page) {		
		return from (adminActor ())
			.list (DataSource.class)
			.list (Category.class)
			.get (DataSource.class, dataSourceId)
			.get (Category.class, categoryId)
			.executeFlat (new Function4<Page<DataSource>, Page<Category>, DataSource, Category, Promise<Result>> () {
				@Override
				public Promise<Result> apply (final Page<DataSource> dataSources, final Page<Category> categories, final DataSource currentDataSource, final Category currentCategory) throws Throwable {
					
					return from (adminActor ())
							.query (new ListSourceDatasets (currentDataSource, currentCategory, search, page))
							.execute (new Function<Page<SourceDatasetStats>, Result> () {
								@Override
								public Result apply (final Page<SourceDatasetStats> sourceDatasets) throws Throwable {
									
									return ok (list.render (sourceDatasets, dataSources.values (), categories.values (), currentDataSource, currentCategory, search));
								}
								
							});
				}
			});
	}
	
	/**
	 * Make a csv file for download. contains information about source datasets. <br>
	 * Comma separated, double quotes around values, encoded to iso-8859-1. 
	 * @param search select source datasets that match search string in their name.
	 * If empty select all sourcedatasets.
	 * @return
	 */
	public static Promise<Result> download(final String search) {
		final String encoding = "iso-8859-1";
		final String filename = "sourcedatasets.csv";
		
		String currentDataSource = null; 
		String currentCategory = null;
		return from (adminActor ())
			.query (new ListSourceDatasets (currentDataSource, currentCategory, search, null))
			.execute (new Function<Page<SourceDatasetStats>, Result> () {
				@Override
				public Result apply (final Page<SourceDatasetStats> sourceDatasetStats) throws Throwable {
					StringBuilder sb = new StringBuilder();
					sb.append("\"id\", \"name\", \"category\", \"datasets\"\n");
					for (SourceDatasetStats sourceDatasetStat : sourceDatasetStats.values()) {
						sb.append("\"" + sourceDatasetStat.sourceDataset().id() + "\",\""+sourceDatasetStat.sourceDataset().name() + "\",\""+sourceDatasetStat.sourceDataset().category().name() + "\",\""+sourceDatasetStat.datasetCount() +"\" \n");
					}
					response().setContentType("application/x-download; charset=" + encoding);  
					response().setHeader("Content-disposition","attachment; filename=" + filename); 
					return ok(sb.toString(), encoding);
				}
			});
	}
}

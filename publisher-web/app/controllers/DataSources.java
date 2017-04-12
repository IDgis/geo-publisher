package controllers;

import static models.Domain.from;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

import com.fasterxml.jackson.databind.node.ObjectNode;

import actions.DefaultAuthenticator;
import actors.Database;
import akka.actor.ActorSelection;

import models.Domain;
import models.Domain.Function;
import models.Domain.Function4;

import nl.idgis.publisher.domain.query.GetMetadata;
import nl.idgis.publisher.domain.query.HarvestDatasources;
import nl.idgis.publisher.domain.query.ListSourceDatasets;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.DataSource;
import nl.idgis.publisher.domain.web.EntityRef;
import nl.idgis.publisher.domain.web.SourceDataset;
import nl.idgis.publisher.domain.web.SourceDatasetStats;

import play.Logger;
import play.Play;
import play.libs.Akka;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

import views.html.datasources.list;

@Security.Authenticated (DefaultAuthenticator.class)
public class DataSources extends Controller {
	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");	

	public static Promise<Result> get(final String sourceDatasetId) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from(database)
			.get(SourceDataset.class, sourceDatasetId)
			.execute(new Function<SourceDataset, Result>() {

				@Override
				public Result apply(SourceDataset sourceDataset) throws Throwable {
					return ok("Hello world!\n" + sourceDataset.name());
				}
				
			});
	}
	
	public static Promise<Result> list (final String search, final Boolean withErrors, final long page) {
		return listByDataSourceAndCategory (null, null, search, withErrors, page);
	}
	
	public static Promise<Result> listByDataSource (final String dataSourceId, final String search, final Boolean withErrors, final long page) {
		return listByDataSourceAndCategory (dataSourceId, null, search, withErrors, page);
	}
	
	public static Promise<Result> listByCategory (final String categoryId, final String search, final Boolean withErrors, final long page) {
		return listByDataSourceAndCategory (null, categoryId, search, withErrors, page);
	}
	
	public static Promise<Result> listByDataSourceAndCategoryJson (final String dataSourceId, final String categoryId) {
		
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from(database)
			.query(new ListSourceDatasets (dataSourceId, categoryId, null, null, null))
			.execute(new Function<Page<SourceDatasetStats>, Result>() {

				@Override
				public Result apply(Page<SourceDatasetStats> sourceDatasetsStats) throws Throwable {
					List<ObjectNode> jsonSourceDatasets = new ArrayList<ObjectNode>();
					
					for(SourceDatasetStats sourceDatasetStats : sourceDatasetsStats.values()) {
						SourceDataset sourceDataset = sourceDatasetStats.sourceDataset();
						
						ObjectNode jsonSourceDataset = Json.newObject();
						jsonSourceDataset.put("id", sourceDataset.id());
						jsonSourceDataset.put("name", sourceDataset.name());
						jsonSourceDataset.put("alternateTitle", sourceDataset.alternateTitle());
						jsonSourceDataset.put("count", sourceDatasetStats.datasetCount());						
						
						jsonSourceDatasets.add(jsonSourceDataset);
					}
					
					ObjectNode result = Json.newObject();
					result.put("sourceDatasets", Json.toJson(jsonSourceDatasets));					
					
					return ok(result);
				}				
			});
	}
	
	public static Promise<Result> listByDataSourceAndCategory (final String dataSourceId, final String categoryId, final String search, final Boolean withErrors, final long page) {
			return listByDataSourceAndCategoryAndSearchString (dataSourceId, categoryId, search, withErrors, page);
	}
	
	private static Promise<Result> listByDataSourceAndCategoryAndSearchString (final String dataSourceId, final String categoryId, final String search, final Boolean withErrors, final long page) {
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
							.query (new ListSourceDatasets (currentDataSource, currentCategory, search, withErrors, page))
							.execute (new Function<Page<SourceDatasetStats>, Result> () {
								@Override
								public Result apply (final Page<SourceDatasetStats> sourceDatasets) throws Throwable {
									
									return ok (list.render (sourceDatasets, dataSources.values (), categories.values (), currentDataSource, currentCategory, search, withErrors));
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
	public static Result download(final String search, final Boolean withErrors, final String separator, final String quote, final String encoding) { 
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		final String filename = "sourcedatasets.csv";
		
		response().setContentType("application/x-download; charset=" + encoding);  
		response().setHeader("Content-disposition", "attachment; filename=" + filename);

		String currentDataSource = null; 
		String currentCategory = null;
		
		long itemsPerPage = 400;
		
		return ok(new StringChunks(encoding) {
			
			private String toLine(List<String> values) {
				return values.stream()
					.map(s -> s == null ? "" : s)
					.map(s -> quote + s.replace(quote, quote + quote) + quote)
					.collect(Collectors.joining(separator)) + "\n";
			}
			
			private Void processPage(Chunks.Out<String> out, Page<SourceDatasetStats> sourceDatasetStats) {
				
				sourceDatasetStats.values().stream()
					.forEach(sourceDatasetStat -> {
						SourceDataset sourceDataset = sourceDatasetStat.sourceDataset();
						EntityRef category = sourceDataset.category(); 
						
						out.write(toLine(Arrays.asList(
							sourceDataset.externalId(),
							sourceDataset.name(),
							category == null
								? ""
								: category.name(),										
							"" + sourceDatasetStat.datasetCount(),
							sourceDatasetStat.lastLogMessage() == null
								? ""
								: Domain.message(sourceDatasetStat.lastLogMessage()))));
					});
				
				if(sourceDatasetStats.currentPage() < sourceDatasetStats.pageCount()) {					
					from(database)
						.query(new ListSourceDatasets (currentDataSource, currentCategory, search, withErrors, sourceDatasetStats.currentPage() + 1, itemsPerPage))
						.execute(nextSourceDatasetStats -> processPage(out, nextSourceDatasetStats))
						.onFailure(t -> {
							Logger.error("generating csv output failed", t);
							out.close();
						});
				} else {
					out.close();
				}
				
				return null;
			}

	        public void onReady(Chunks.Out<String> out) {
	        	out.write(toLine(Arrays.asList("id", "name", "category", "datasets", "error")));
	        	
	        	from(database)
					.query(new ListSourceDatasets (currentDataSource, currentCategory, search, withErrors, 1l, itemsPerPage))
					.execute(sourceDatasetStats -> processPage(out, sourceDatasetStats))
					.onFailure(t -> {
						Logger.error("generating csv output failed", t);
						out.close();
					});
	        }
	        
	    });	    
	}
	
	public static Promise<Result> refreshDatasources () {
		return refreshDatasource (null);
	}
	
	public static Promise<Result> refreshDatasource (final String datasourceId) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from (database)
		 	.query (new HarvestDatasources (datasourceId))
		 	.execute (new Function<Boolean, Result> () {
				@Override
				public Result apply (final Boolean result) throws Throwable {
					final ObjectNode response = Json.newObject ();
					
					response.put ("success", result);
					
					if (result) {
						return ok (response);
					} else {
						return internalServerError (response);
					}
				}
		 	});
	}
}

package controllers;

import static models.Domain.from;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

import models.Domain.Function;
import models.Domain.Function2;
import models.Domain.Function3;
import nl.idgis.publisher.domain.query.ListColumns;
import nl.idgis.publisher.domain.query.ListDatasets;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.DataSource;
import nl.idgis.publisher.domain.web.Dataset;
import nl.idgis.publisher.domain.web.PutDataset;
import nl.idgis.publisher.domain.web.SourceDataset;
import play.Logger;
import play.Play;
import play.libs.Akka;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.datasets.form;
import views.html.datasets.list;
import actions.DefaultAuthenticator;
import actors.Database;
import akka.actor.ActorSelection;
import views.html.datasets.columns;
import play.data.Form;
import play.data.validation.Constraints;

@Security.Authenticated (DefaultAuthenticator.class)
public class Datasets extends Controller {
	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");

	public static Promise<Result> list (long page) {
		return listByCategoryAndMessages(null, false, page);
	}
	
	
	public static Promise<Result> listWithMessages (long page) {
		return listByCategoryAndMessages(null, true, page);
	}
	
	public static Promise<Result> listByCategory (String categoryId, long page) {
		return listByCategoryAndMessages(categoryId, false, page);
	}
	
	
	public static Promise<Result> listByCategoryWithMessages (String categoryId, long page) {
		return listByCategoryAndMessages(categoryId, true, page);
	}
	
	public static Promise<Result> delete(final String datasetId){
		System.out.println("delete dataset " + datasetId);
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		from(database).delete(Dataset.class, datasetId); 
		return list(1);
	}
	
	public static Promise<Result> update(){
		// TODO construct putdataset from form (putdataset.id != null)
		PutDataset putDataset = new PutDataset("1", "MyUpdatedDataset", "SomeSourceDataset", new ArrayList<Column>());		
		System.out.println("update dataset " + putDataset.getDatasetIdentification());
		
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		from(database).put(putDataset); 
		
		return list(1);
	}
	
	public static Promise<Result>  create() {
		// TODO construct putdataset from form (putdataset.id == null)
		PutDataset putDataset = new PutDataset(null, "MyCreatedDataset", "SomeSourceDataset", new ArrayList<Column>());		
		System.out.println("create dataset " + putDataset.getDatasetIdentification());
		
		final ActorSelection database = Akka.system().actorSelection (databaseRef);

		from(database).put(putDataset); 
		
		return list(1);
	}
	
	private static Promise<Result> renderCreateForm (final Form<DatasetForm> datasetForm) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from(database)
			.list(DataSource.class)
			.list(Category.class)
			.execute(new Function2<Page<DataSource>, Page<Category>, Result>() {
				@Override
				public Result apply(Page<DataSource> dataSources, Page<Category> categories) throws Throwable {
					return ok (form.render (dataSources, categories, datasetForm));
				}
			});
	}
	
	public static Promise<Result> createForm () {

		final Form<DatasetForm> datasetForm = Form.form (DatasetForm.class);
		
		return renderCreateForm (datasetForm);
	}
	
	public static Promise<Result> submitCreate () {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		final Form<DatasetForm> datasetForm = Form.form (DatasetForm.class).bindFromRequest ();
		
		if (datasetForm.hasErrors ()) {
			return renderCreateForm (datasetForm);
		}
		
		final DatasetForm dataset = datasetForm.get ();
		
		return from (database)
				.get (DataSource.class, dataset.getDataSourceId ())
				.get (Category.class, dataset.getCategoryId ())
				.get (SourceDataset.class, dataset.getSourceDatasetId ())
				.executeFlat (new Function3<DataSource, Category, SourceDataset, Promise<Result>> () {
					@Override
					public Promise<Result> apply (final DataSource dataSource, final Category category, final SourceDataset sourceDataset) throws Throwable {
						Logger.debug ("dataSource: " + dataSource);
						Logger.debug ("category: " + category);
						Logger.debug ("sourceDataset: " + sourceDataset);

						return renderCreateForm (datasetForm);
					}
				});
	}
	
	public static Promise<Result> editForm (final String datasetId) {
		return Promise.pure ((Result) ok ());
	}
	
	public static Promise<Result> submitEdit (final String datasetId) {
		return Promise.pure ((Result) ok ());
	}
	
	public static Promise<Result> listByCategoryAndMessages (final String categoryId, final boolean listWithMessages, final long page) {
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
							.query (new ListDatasets (currentCategory, page))
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
	
	public static Promise<Result> listColumns(final String dataSourceId, final String sourceDatasetId) {
		
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return 
			from(database)
				.query(new ListColumns(dataSourceId, sourceDatasetId))
				.execute(new Function<List<Column>, Result>() {
	
					@Override
					public Result apply(List<Column> c) throws Throwable {
						return ok(columns.render(c, null));
					}
				});
	}

	public static class DatasetForm {
		
		@Constraints.Required
		@Constraints.MinLength (1)
		private String name;

		@Constraints.Required
		private String dataSourceId;
		
		@Constraints.Required
		private String categoryId;
		
		@Constraints.Required
		private String sourceDatasetId;
		
		private Map<String, String> columns;
		
		public String getName() {
			return name;
		}
		
		public void setName (final String name) {
			this.name = name;
		}
		
		public String getDataSourceId () {
			return dataSourceId;
		}

		public void setDataSourceId (final String dataSourceId) {
			this.dataSourceId = dataSourceId;
		}

		public String getCategoryId() {
			return categoryId;
		}
		
		public void setCategoryId (final String categoryId) {
			this.categoryId = categoryId;
		}
		
		public String getSourceDatasetId () {
			return sourceDatasetId;
		}
		
		public void setSourceDatasetId (final String sourceDatasetId) {
			this.sourceDatasetId = sourceDatasetId;
		}
		
		public Map<String, String> getColumns () {
			return columns;
		}
		
		public void setColumns (final Map<String, String> columns) {
			this.columns = columns;
		}
	}
}

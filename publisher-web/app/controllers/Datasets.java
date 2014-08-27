package controllers;

import static models.Domain.from;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Domain.Constant;
import models.Domain.Function;
import models.Domain.Function2;
import models.Domain.Function4;
import nl.idgis.publisher.domain.query.DomainQuery;
import nl.idgis.publisher.domain.query.ListDatasetColumns;
import nl.idgis.publisher.domain.query.ListDatasets;
import nl.idgis.publisher.domain.query.ListSourceDatasetColumns;
import nl.idgis.publisher.domain.query.ListSourceDatasets;
import nl.idgis.publisher.domain.query.RefreshDataset;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.DataSource;
import nl.idgis.publisher.domain.web.Dataset;
import nl.idgis.publisher.domain.web.Filter;
import nl.idgis.publisher.domain.web.Filter.OperatorType;
import nl.idgis.publisher.domain.web.PutDataset;
import nl.idgis.publisher.domain.web.SourceDataset;
import nl.idgis.publisher.domain.web.SourceDatasetStats;
import play.Logger;
import play.Play;
import play.data.Form;
import play.data.validation.Constraints;
import play.data.validation.ValidationError;
import play.libs.Akka;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.datasets.columns;
import views.html.datasets.form;
import views.html.datasets.list;
import actions.DefaultAuthenticator;
import actors.Database;
import akka.actor.ActorSelection;

import com.fasterxml.jackson.databind.node.ObjectNode;

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
	
	public static Promise<Result> scheduleRefresh(String datasetId) {
		
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from(database)
			.query(new RefreshDataset(datasetId))
			.execute(new Function<Boolean, Result>() {

				@Override
				public Result apply(Boolean b) throws Throwable {
					final ObjectNode result = Json.newObject ();
					
					if (b) {
						result.put ("result", "ok");
						
						return ok (result);
					} else {
						result.put ("result", "failed");
						
						return internalServerError (result);
					}
				}
			});
	}
	
	public static Promise<Result> delete(final String datasetId){
		System.out.println("delete dataset " + datasetId);
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		from(database).delete(Dataset.class, datasetId)
			.execute(new Function<Response<?>, Result>() {

				@Override
				public Result apply(Response<?> a) throws Throwable {
					// TODO Auto-generated method stub
					System.out.println("apply delete: " + a);
					return null;
				}
			});
 
		return list(1);
	}

	private static DomainQuery<Page<SourceDatasetStats>> listSourceDatasets (final String dataSourceId, final String categoryId) {
		if (dataSourceId == null || categoryId == null || dataSourceId.isEmpty () || categoryId.isEmpty ()) {
			return new Constant<Page<SourceDatasetStats>> (new Page.Builder<SourceDatasetStats> ().build ());
		}
		
		return new ListSourceDatasets (dataSourceId, categoryId); 
	}
	
	private static DomainQuery<List<Column>> listSourceDatasetColumns (final String dataSourceId, final String sourceDatasetId) {
		if (dataSourceId == null || sourceDatasetId == null || dataSourceId.isEmpty () || sourceDatasetId.isEmpty ()) {
			return new Constant<List<Column>> (Collections.<Column>emptyList ());
		}
		
		return new ListSourceDatasetColumns (dataSourceId, sourceDatasetId);
	}
	
	private static DomainQuery<List<Column>> listDatasetColumns (final String datasetId) {
		if (datasetId == null || datasetId.isEmpty ()) {
			return new Constant<List<Column>> (Collections.<Column>emptyList ());
		}
		
		return new ListDatasetColumns (datasetId);
	}
	
	private static Promise<Result> renderCreateForm (final Form<DatasetForm> datasetForm) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from(database)
			.list(DataSource.class)
			.list(Category.class)
			.query (listSourceDatasets (datasetForm.field ("dataSourceId").value (), datasetForm.field ("categoryId").value ()))
			.query (listSourceDatasetColumns (datasetForm.field ("dataSourceId").value (), datasetForm.field ("sourceDatasetId").value ()))
			.execute(new Function4<Page<DataSource>, Page<Category>, Page<SourceDatasetStats>, List<Column>, Result>() {
				@Override
				public Result apply(Page<DataSource> dataSources, Page<Category> categories, final Page<SourceDatasetStats> sourceDatasets, final List<Column> columns) throws Throwable {
					Logger.debug ("Create form: #datasources=" + dataSources.pageCount() + 
							", #categories=" + categories.pageCount() + 
							", #sourcedatasets=" + sourceDatasets.pageCount() + 
							", #columns: " + columns.size () + 
							", datasetForm: " + datasetForm);
					return ok (form.render (dataSources, categories, sourceDatasets, columns, datasetForm));
				}
			});
	}
	
	public static Promise<Result> createForm () {
		Logger.debug ("createForm");
		final Form<DatasetForm> datasetForm = Form.form (DatasetForm.class).fill (new DatasetForm ());
		
		return renderCreateForm (datasetForm);
	}
	
	public static Promise<Result> createFormForSourceDataset (final String sourceDatasetId) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from (database)
			.get (SourceDataset.class, sourceDatasetId)
			.executeFlat (new Function<SourceDataset, Promise<Result>> () {
				@Override
				public Promise<Result> apply (final SourceDataset sourceDataset) throws Throwable {
					if (sourceDataset == null) {
						return Promise.pure ((Result) notFound ());
					}
					
					final DatasetForm form = new DatasetForm ();
					
					form.setName (sourceDataset.name ());
					form.setSourceDatasetId (sourceDatasetId);
					form.setDataSourceId (sourceDataset.dataSource ().id ());
					form.setCategoryId (sourceDataset.category ().id ());
					
					return renderCreateForm (Form.form (DatasetForm.class).fill (form));
				}
			});
	}
	
	public static Promise<Result> submitCreate () {
		Logger.debug ("submitCreate");
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
				.query (new ListSourceDatasetColumns (dataset.getDataSourceId (), dataset.getSourceDatasetId ()))
				.executeFlat (new Function4<DataSource, Category, SourceDataset, List<Column>, Promise<Result>> () {
					@Override
					public Promise<Result> apply (final DataSource dataSource, final Category category, final SourceDataset sourceDataset, final List<Column> sourceColumns) throws Throwable {
						Logger.debug ("dataSource: " + dataSource);
						Logger.debug ("category: " + category);
						Logger.debug ("sourceDataset: " + sourceDataset);
						
						// TODO: Validate dataSource, category, sourceDataset and columns!
						// Validate the filter:
						if (!dataset.getFilterConditions ().isValid (sourceColumns)) {
							datasetForm.reject (new ValidationError ("filterConditions", "Het opgegeven filter is ongeldig"));
							return renderCreateForm (datasetForm);
						}
						
						// Create the list of selected columns:
						final List<Column> columns = new ArrayList<> ();
						for (final Column column: sourceColumns) {
							if (dataset.getColumns ().containsKey (column.getName ())) {
								columns.add (column);
							}
						}

						final PutDataset putDataset = new PutDataset (CrudOperation.CREATE,
								dataset.getId (), 
								dataset.getName (), 
								sourceDataset.id (), 
								columns,
								dataset.getFilterConditions ()
							);
						
						Logger.debug ("create dataset " + putDataset);
						
						return from (database)
							.put(putDataset)
							.executeFlat (new Function<Response<?>, Promise<Result>> () {
								@Override
								public Promise<Result> apply (final Response<?> response) throws Throwable {
									if (CrudResponse.NOK.equals (response.getOperationresponse ())) {
										datasetForm.reject ("Er bestaat al een dataset met tabelnaam " + dataset.getId ());
										return renderCreateForm (datasetForm);
									}
									
									flash ("success", "Dataset " + dataset.getName () + " is toegevoegd.");
									
									return Promise.pure (redirect (routes.Datasets.list (0)));
								}
							});
					}
				});
	}
	
	private static Promise<Result> renderEditForm (final Form<DatasetForm> datasetForm) {
		//TODO 
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from(database)
			.list(DataSource.class)
			.list(Category.class)
			.query (listSourceDatasets (datasetForm.field ("dataSourceId").value (), datasetForm.field ("categoryId").value ()))
//			.query (listDatasetColumns (datasetForm.field ("id").value ()))
			.query (listSourceDatasetColumns (datasetForm.field ("dataSourceId").value (), datasetForm.field ("sourceDatasetId").value ()))
			.execute(new Function4<Page<DataSource>, Page<Category>, Page<SourceDatasetStats>, List<Column>, Result>() {
				@Override
				public Result apply(Page<DataSource> dataSources, Page<Category> categories, final Page<SourceDatasetStats> sourceDatasets, final List<Column> columns) throws Throwable {
					Logger.debug ("Edit form: #datasources=" + dataSources.pageCount() + 
							", #categories=" + categories.pageCount() + 
							", #sourcedatasets=" + sourceDatasets.pageCount() + 
							", #columns: " + columns.size ());
					return ok (form.render (dataSources, categories, sourceDatasets, columns, datasetForm));
				}
			});
	}
	
	public static Promise<Result> editForm (final String datasetId) {
		//TODO
		Logger.debug ("editForm");

		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from (database)
			.get (Dataset.class, datasetId)
			.query (listDatasetColumns (datasetId))
			.executeFlat (new Function2<Dataset, List<Column>, Promise<Result>> () {
				@Override
				public Promise<Result> apply (final Dataset ds, final List<Column> columns) throws Throwable {
					return from (database)
						.get (SourceDataset.class, ds.sourceDataset().id())
						.executeFlat (new Function<SourceDataset, Promise<Result>> () {
							@Override
							public Promise<Result> apply (final SourceDataset sds) throws Throwable {
								final Form<DatasetForm> datasetForm = Form
										.form (DatasetForm.class)
										.fill (new DatasetForm (ds, sds.dataSource().id(), columns));
								
								Logger.debug ("Edit datasetForm: " + datasetForm);						
								return renderEditForm (datasetForm);
							}
						});
				}
			});
//		return Promise.pure ((Result) ok ());
	}
	
	public static Promise<Result> submitEdit (final String datasetId) {
		Logger.debug ("submitEdit");

		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		final Form<DatasetForm> datasetForm = Form.form (DatasetForm.class).bindFromRequest ();
		
		if (datasetForm.hasErrors ()) {
			return renderEditForm (datasetForm);
		}
		
		final DatasetForm dataset = datasetForm.get ();
		
		return from (database)
				.get (DataSource.class, dataset.getDataSourceId ())
				.get (Category.class, dataset.getCategoryId ())
				.get (SourceDataset.class, dataset.getSourceDatasetId ())
				.query (new ListSourceDatasetColumns (dataset.getDataSourceId (), dataset.getSourceDatasetId ()))
				.executeFlat (new Function4<DataSource, Category, SourceDataset, List<Column>, Promise<Result>> () {
					@Override
					public Promise<Result> apply (final DataSource dataSource, final Category category, final SourceDataset sourceDataset, final List<Column> sourceColumns) throws Throwable {
						Logger.debug ("dataSource: " + dataSource);
						Logger.debug ("category: " + category);
						Logger.debug ("sourceDataset: " + sourceDataset);
						
						// TODO: Validate dataSource, category, sourceDataset!
						
						// Validate the columns used by the filter:
						if (!dataset.getFilterConditions ().isValid (sourceColumns)) {
							datasetForm.reject (new ValidationError ("filterConditions", "Het opgegeven filter is ongeldig"));
							return renderEditForm (datasetForm);
						}
						
						// Create the list of selected columns:
						final List<Column> columns = new ArrayList<> ();
						for (final Column column: sourceColumns) {
							if (dataset.getColumns ().containsKey (column.getName ())) {
								columns.add (column);
							}
						}

						final PutDataset putDataset = new PutDataset (CrudOperation.UPDATE,
								dataset.getId (), 
								dataset.getName (), 
								sourceDataset.id (), 
								columns,
								dataset.getFilterConditions ()
							);
						
						Logger.debug ("update dataset " + putDataset);
						
						return from (database)
							.put(putDataset)
							.executeFlat (new Function<Response<?>, Promise<Result>> () {
								@Override
								public Promise<Result> apply (final Response<?> response) throws Throwable {
									if (CrudResponse.NOK.equals (response.getOperationresponse ())) {
										datasetForm.reject ("dataset kon niet worden geupdate: " + dataset.getName ());
										return renderEditForm (datasetForm);
									}
									
									flash ("success", "Dataset " + dataset.getName () + " is aangepast.");
									
									return Promise.pure (redirect (routes.Datasets.list (0)));
								}
							});
					}
				});
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
	
	public static Promise<Result> listColumnsAction (final String dataSourceId, final String sourceDatasetId) {
		
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return 
			from(database)
				.query(new ListSourceDatasetColumns(dataSourceId, sourceDatasetId))
				.execute(new Function<List<Column>, Result>() {
	
					@Override
					public Result apply(List<Column> c) throws Throwable {
						return ok(columns.render(c, null));
					}
				});
	}
	
	public static Promise<Result> getDatasetJson (final String datasetId) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);

		Logger.debug ("getDatasetJson: " + datasetId);
		
		return from (database)
			.get (Dataset.class, datasetId)
			.execute (new Function<Dataset, Result> () {
				@Override
				public Result apply (final Dataset ds) throws Throwable {
					final ObjectNode result = Json.newObject ();
					
					result.put ("id", datasetId);
					
					if (ds == null) {
						result.put ("status", "notfound");
						return ok (result);
					}
					
					result.put ("status", "ok");
					result.put ("dataset", Json.toJson (ds));
					
					return ok (result);
				}
			});
	}

	private static Filter emptyFilter () {
		final Filter.OperatorExpression andExpression = new Filter.OperatorExpression (OperatorType.AND, Collections.<Filter.FilterExpression>emptyList ());
		final Filter.OperatorExpression orExpression = new Filter.OperatorExpression (OperatorType.OR, Arrays.<Filter.FilterExpression>asList (new Filter.FilterExpression[] { andExpression }));
		
		return new Filter (orExpression);
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

		@Constraints.Required
		private Map<String, String> columns;

		@Constraints.Required
		@Constraints.MinLength (3)
		@Constraints.Pattern ("^[a-zA-Z_][0-9a-zA-Z_]+$")
		private String id;

		@Constraints.Required
		private Filter filterConditions;

		public DatasetForm () {
			filterConditions = emptyFilter ();
		}
		
		public DatasetForm (final Dataset ds, String dataSourceId, List<Column> columns) {
			setName (ds.name ());
			setDataSourceId (dataSourceId);
			setCategoryId (ds.category ().id ());
			setSourceDatasetId (ds.sourceDataset ().id ());
			Map<String, String> map = new HashMap<String, String>();
			for (Column column : columns) {
				map.put(column.getName(), column.getDataType().toString());
			}			
			setColumns (map);
			setId (ds.id ());
			setFilterConditions (ds.filterConditions ());
		}
		
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

		public String getId () {
			return id;
		}

		public void setId (final String id) {
			this.id = id;
		}

		public Filter getFilterConditions () {
			return filterConditions;
		}

		public void setFilterConditions (final Filter filterConditions) {
			this.filterConditions = filterConditions;
		}

		@Override
		public String toString() {
			return "DatasetForm [name=" + name + ", dataSourceId="
					+ dataSourceId + ", categoryId=" + categoryId
					+ ", sourceDatasetId=" + sourceDatasetId + ", columns="
					+ columns + ", id=" + id + "]";
		}
	}
}

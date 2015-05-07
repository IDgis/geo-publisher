package controllers;

import static models.Domain.from;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import models.Domain;
import models.Domain.Function;
import models.Domain.Function2;
import models.Domain.Function3;
import models.Domain.Function4;
import models.Domain.Function5;
import nl.idgis.publisher.domain.query.GetLayerParentGroups;
import nl.idgis.publisher.domain.query.GetLayerParentServices;
import nl.idgis.publisher.domain.query.GetLayerRef;
import nl.idgis.publisher.domain.query.GetLayerServices;
import nl.idgis.publisher.domain.query.ListLayerKeywords;
import nl.idgis.publisher.domain.query.ListLayerStyles;
import nl.idgis.publisher.domain.query.ListLayers;
import nl.idgis.publisher.domain.query.ListStyles;
import nl.idgis.publisher.domain.query.PutLayerKeywords;
import nl.idgis.publisher.domain.query.PutLayerStyles;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.web.Dataset;
import nl.idgis.publisher.domain.web.Layer;
import nl.idgis.publisher.domain.web.LayerGroup;
import nl.idgis.publisher.domain.web.Service;
import nl.idgis.publisher.domain.web.Style;
import nl.idgis.publisher.domain.web.TiledLayer;
import play.Logger;
import play.Play;
import play.api.mvc.Call;
import play.data.Form;
import play.data.validation.Constraints;
import play.libs.Akka;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.Security;
import views.html.helper.groupStructureItem;
import views.html.layers.form;
import views.html.layers.list;
import views.html.layers.layerPagerHeader;
import views.html.layers.layerPagerBody;
import views.html.layers.layerPagerFooter;
import actions.DefaultAuthenticator;
import akka.actor.ActorSelection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import scala.runtime.AbstractFunction1;

@Security.Authenticated (DefaultAuthenticator.class)
public class Layers extends GroupsLayersCommon {
	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");
	private final static String ID="#CREATE_LAYER#";
	
	private static Promise<Result> renderCreateForm (final Form<LayerForm> layerForm) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		return from (database)
				.query (new ListStyles (1l, null, null))
				.execute (new Function<Page<Style>, Result> () {

					@Override
					public Result apply (final Page<Style> allStyles) throws Throwable {
						return ok (form.render (layerForm, true, allStyles, "", null, null, ""));
					}
				});
	}
	
	public static Promise<Result> submitCreateUpdate () {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		return from (database)
			.list (LayerGroup.class)
			.list (Layer.class)
			.list (Service.class)
			.executeFlat (new Function3<Page<LayerGroup>, Page<Layer>, Page<Service>, Promise<Result>> () {
	
				@Override
				public Promise<Result> apply (final Page<LayerGroup> groups, final Page<Layer> layers, final Page<Service> services) throws Throwable {
					final Form<LayerForm> form = Form.form (LayerForm.class).bindFromRequest ();
					
					// validation start
					if (form.field("id").value().equals(ID) && form.field ("name").valueOr (null) != null){
						for (LayerGroup layerGroup : groups.values()) {
							if (form.field("name").value().equals(layerGroup.name())){
								form.reject("name", Domain.message("web.application.page.layers.form.field.name.validation.groupexists.error"));
							}
						}
						for (Layer layer : layers.values()) {
							if (form.field("name").value().equals(layer.name())){
								form.reject("name", Domain.message("web.application.page.layers.form.field.name.validation.layerexists.error"));
							}
						}
						for (final Service service: services.values ()) {
							if (form.field ("name").value ().equals (service.name ())) {
								form.reject ("name", "web.application.page.layers.form.field.name.validation.serviceexists.error");
							}
						}
					}
					if (form.field("styles").value().isEmpty() || form.field("styles").value().equals("[]")) {
						Logger.debug ("Empty style list");
						form.reject("styles", Domain.message("web.application.page.layers.form.field.styles.validation.error"));
					} else {
						Logger.debug ("Form style list " + form.field("styles").value());
					}
					
					if (form.hasErrors ()) {
						Logger.debug ("LayerForm errors " + form.errorsAsJson().toString());
						return renderCreateForm (form);
					}
					// validation end
					
 					// parse the list of (style.name, style.id) from the json string in the view form
					String layerStyleList = form.get().getStyles();
					
 					final List<String> styleIds = new ArrayList<> ();
					for (final JsonNode n: Json.parse (layerStyleList)) {
						// get only the second element (style.id)
						styleIds.add (n.get (1).asText ());
 					}
					Logger.debug ("layerStyleList: " + styleIds.toString ());
					
					final LayerForm layerForm = form.get ();
					final Layer layer = new Layer(layerForm.getId(), layerForm.getName(), layerForm.title, 
							layerForm.abstractText,layerForm.datasetId, layerForm.datasetName,
							(layerForm.enabled ? layerForm.getTiledLayer() : null), layerForm.getKeywords(), layerForm.getStyleList(), false);
					Logger.debug ("Create Update layerForm: " + layerForm);						
					
					return from (database)
						.put(layer)
						.executeFlat (new Function<Response<?>, Promise<Result>> () {
							@Override
							public Promise<Result> apply (final Response<?> response) throws Throwable {
								// Get the id of the layer we just put 
								String layerId = response.getValue().toString();
								PutLayerKeywords putLayerKeywords = 
									new PutLayerKeywords (layerId, layerForm.getKeywords()==null?new ArrayList<String>():layerForm.getKeywords());
								PutLayerStyles putLayerStyles = new PutLayerStyles(layerId, styleIds);															
								return from (database)
									.query(putLayerStyles)
									.query(putLayerKeywords)
									.executeFlat (new Function2<Response<?>, Response<?>, Promise<Result>> () {
										@Override
										public Promise<Result> apply (final Response<?> responseStyles, final Response<?> responseKeywords) throws Throwable {
										
											if (CrudOperation.CREATE.equals (responseStyles.getOperation())) {
												Logger.debug ("Created layer " + layer);
												flash ("success", Domain.message("web.application.page.layers.name") + " " + layer.name() + " is " + Domain.message("web.application.added").toLowerCase());
											}else{
												Logger.debug ("Updated layer " + layer);
												flash ("success", Domain.message("web.application.page.layers.name") + " " + layer.name() + " is " + Domain.message("web.application.updated").toLowerCase());
											}
											return Promise.pure (redirect (routes.Layers.list (null, 1)));
										}
									});
							}
						});
				}
			});
	}
	
	public static Promise<Result> list (final String query, final long page) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);

		Logger.debug ("list Layers ");
		
		return from (database)
			.query (new ListLayers (page, query))
			.execute (new Function<Page<Layer>, Result> () {
				@Override
				public Result apply (final Page<Layer> layers) throws Throwable {
					Logger.debug ("Layer list : #" + layers.values().size());
					return ok (list.render (layers, query));
				}
			});
	}
	
	public static Promise<Result> structureItem(String layerId, boolean showStyleSelect) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from(database)
			.query(new GetLayerRef(layerId))
			.execute(layerRef ->
				ok(groupStructureItem.render(layerRef, showStyleSelect)));
	}
	
	public static Promise<Result> listJson (final String query, final long page) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);

		return from (database)
			.query (new ListLayers (page, query))
			.execute (new Function<Page<Layer>, Result> () {
				@Override
				public Result apply (final Page<Layer> layers) throws Throwable {
					final ObjectNode result = Json.newObject ();
					
					result.put ("header", layerPagerHeader.render (query).toString ());
					result.put ("body", layerPagerBody.render (layers).toString ());
					result.put ("footer", layerPagerFooter.render (layers, new AbstractFunction1<Long, Call>() {

						@Override
						public Call apply(Long page) {
							return routes.Layers.listJson(query, page);
						}
						
					}).toString ());

					return ok (result);
				}
			});
	}

	/**
	 * Create a new layer given a dataset id.
	 * @param datasetId
	 * @return
	 */
	public static Promise<Result> create (String datasetId) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		Logger.debug ("create Layer with dataset id: " + datasetId);
		
		LayerForm layerForm = new LayerForm ();
		// The list of styles for this layer is initially empty
		layerForm.setStyleList(new ArrayList<Style>());
		// set mimeformats to default
		layerForm.setMimeFormats(null);
		
		return from (database)
				.get (Dataset.class, datasetId)
				.executeFlat (new Function<Dataset, Promise<Result>> () {
					@Override
					public Promise<Result> apply (final Dataset dataset) throws Throwable {
						Logger.debug ("dataset: " + dataset.name());
//						return ok (list.render (layers));
						layerForm.setDatasetId(dataset.id());
						layerForm.setDatasetName(dataset.name());
						layerForm.setName(dataset.name().replace(' ', '_'));						
						final Form<LayerForm> formLayerForm = Form.form (LayerForm.class).fill (layerForm );
						return renderCreateForm (formLayerForm);
					}
				});
	}

	public static Promise<Result> edit (final String layerId) {
		Logger.debug ("edit Layer: " + layerId);
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from (database)
			.get (Layer.class, layerId)
			.query (new ListStyles (1l, null, null))
			.query(new GetLayerServices(layerId))
			.query(new GetLayerParentGroups(layerId))
			.query(new GetLayerParentServices(layerId))
			.executeFlat (new Function5<Layer, Page<Style>, List<String>, Page<LayerGroup>, Page<Service>, Promise<Result>> () {

				@Override
				public Promise<Result> apply (final Layer layer, final Page<Style> allStyles, final List<String> serviceIds, final Page<LayerGroup> parentGroups, final Page<Service> parentServices) throws Throwable {
					String serviceId;
					if (serviceIds==null || serviceIds.isEmpty()){
						serviceId="";
					} else {
						Logger.debug ("Services for layer " + layer.name() + ": # " + serviceIds.size());								
						// get the first service in the list for preview
						serviceId=serviceIds.get(0);
					}
					return from (database)
							.get(Service.class, serviceId)
							.execute (new Function<Service, Result> () {

							@Override
							public Result apply (final Service service) throws Throwable {
								
								LayerForm layerForm = new LayerForm (layer);
								Logger.debug ("tiledlayer present: " + layer.tiledLayer().isPresent() + ", enabled: " + layerForm.getEnabled());
								
								List<Style> layerStyles ;
								if (layer.styles() == null){ 
									layerStyles = new ArrayList<Style>();
								} else {
									layerStyles = layer.styles(); 
								}
								layerForm.setStyleList(layerStyles);
									
								final Form<LayerForm> formLayerForm = Form
										.form (LayerForm.class)
										.fill (layerForm);
								
								Logger.debug ("Edit layerForm: " + layerForm);						
								
								// build a json string with list of styles (style.name, style.id) 
								final ArrayNode arrayNode = Json.newObject ().putArray ("styleList");
								for (final Style style: layerStyles) {
									final ArrayNode styleNode = arrayNode.addArray ();
									styleNode.add (style.name ());
									styleNode.add (style.id ());
								}					
								
								final String layerStyleListString = Json.stringify (arrayNode);
								Logger.debug ("allStyles: #" + allStyles.values().size());
								Logger.debug ("layerStyles: #" + layerStyles.size());
								Logger.debug ("layerStyles List: " + layerStyleListString);
								// build a layer preview string
								final String previewUrl ;
								if (service==null){
									previewUrl = null;
								} else {
									previewUrl = makePreviewUrl(service.name(), layer.name());
								}
								return ok (form.render (formLayerForm, false, allStyles, layerStyleListString, parentGroups, parentServices, previewUrl));
							}
						});
				}
			});
	}

	public static Promise<Result> delete(final String layerId){
		Logger.debug ("delete Layer " + layerId);
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from(database).delete(Layer.class, layerId)
		.execute(new Function<Response<?>, Result>() {
			
			@Override
			public Result apply(Response<?> a) throws Throwable {
				return redirect (routes.Layers.list (null, 1));
			}
		});
		
	}
	
	public static class LayerForm extends TiledLayerForm{
		
		@Constraints.Required
		private String id;
		
		@Constraints.Required (message = "web.application.page.layers.form.field.name.validation.required")
		@Constraints.MinLength (value = 3, message = "web.application.page.layers.form.field.name.validation.length")
		@Constraints.Pattern (value = "^[a-zA-Z][a-zA-Z0-9\\-\\_]+$", message = "web.application.page.layers.form.field.name.validation.error")
		private String name;

		private String title;
		private String abstractText;
		private List<String> keywords;
		private String datasetId;
		private String datasetName;
		/**
		 * List of styles in this layer
		 */
		private List<Style> styleList;
		/**
		 * Json array of all styles in the layer
		 */
		private String styles;
		private Boolean enabled = false;

		
		public LayerForm(){
			super();
			this.id = ID;
			this.keywords = new ArrayList<String>();
		}
		
		public LayerForm(Layer layer){
			super(layer.tiledLayer().isPresent()?layer.tiledLayer().get():null);
			this.id = layer.id();
			this.name = layer.name();
			this.title = layer.title();
			this.abstractText = layer.abstractText();
			this.datasetId = layer.datasetId();
			this.datasetName = layer.datasetName();
			this.keywords = layer.getKeywords();
			this.styleList = layer.styles();
			this.enabled = layer.tiledLayer().isPresent();
		}

		public String getId() {
			return this.id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getAbstractText() {
			return abstractText;
		}

		public void setAbstractText(String abstractText) {
			this.abstractText = abstractText;
		}

		public List<String> getKeywords() {
			return keywords;
		}

		public void setKeywords(List<String> keywords) {
			if (keywords==null){
				this.keywords = new ArrayList<String>();
			}else{
				this.keywords = keywords;
			}
		}

		public List<Style> getStyleList() {
			return styleList;
		}

		public void setStyleList(List<Style> styles) {
			this.styleList = styleList;
		}

		public String getStyles() {
			return styles;
		}

		public void setStyles(String styles) {
			this.styles = styles;
		}

		public String getDatasetName() {
			return datasetName;
		}

		public void setDatasetName(String datasetName) {
			this.datasetName = datasetName;
		}

		public String getDatasetId() {
			return datasetId;
		}

		public void setDatasetId(String datasetId) {
			this.datasetId = datasetId;
		}

		public Boolean getEnabled() {
			return enabled;
		}

		public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}

		@Override
		public String toString() {
			return "LayerForm [id=" + getId() + ", name=" + getName() + ", title=" + title + ", abstractText=" + abstractText
					+ ", keywords=" + keywords + ", datasetId=" + datasetId
					+ ", datasetName=" + datasetName + ", styleList=" + styles + ", enabled=" + enabled + ", toString()=" + super.toString() + "]";
		}
		
	}
}

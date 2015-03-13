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
import play.data.Form;
import play.data.validation.Constraints;
import play.libs.Akka;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.Security;
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

import controllers.Tiledlayers.TiledLayerForm;

@Security.Authenticated (DefaultAuthenticator.class)
public class Layers extends GroupsLayersCommon {
	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");
	private final static String ID="#CREATE_LAYER#";
	
	
	private static Promise<Result> renderCreateForm (final Form<LayerForm> layerForm) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		return from (database)
				.query (new ListStyles (1l, null))
				.execute (new Function<Page<Style>, Result> () {

					@Override
					public Result apply (final Page<Style> allStyles) throws Throwable {
						return ok (form.render (layerForm, true, allStyles, "", ""));
					}
				});
	}
	
	public static Promise<Result> submitCreateUpdate () {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		return from (database)
			.list (LayerGroup.class)
			.list (Layer.class)
			.executeFlat (new Function2<Page<LayerGroup>, Page<Layer>, Promise<Result>> () {
	
				@Override
				public Promise<Result> apply (final Page<LayerGroup> groups, final Page<Layer> layers) throws Throwable {
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
					}
					if (form.field("styles").value().length() == 0 ) 
						form.reject("styles", Domain.message("web.application.page.layers.form.field.styles.validation.error"));
					if (form.hasErrors ()) {
						return renderCreateForm (form);
					}
					// validation end
					
					// parse the list of (style.name, style.id) from the json string in the view form
					final List<String> styleIds = new ArrayList<> ();
					List<Style> layerStyleList = form.get().getStyles();
					for (Style style : layerStyleList) {
						styleIds.add (style.definition());
						
					}
					
						Logger.debug ("layerStyleList: " + styleIds.toString ());
					
					final LayerForm layerForm = form.get ();
					final Layer layer = new Layer(layerForm.id, layerForm.name, layerForm.title, 
							layerForm.abstractText,layerForm.published,layerForm.datasetId, layerForm.datasetName,
							layerForm.getTiledLayer(), layerForm.getKeywords(), layerForm.getStyles());
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
											return Promise.pure (redirect (routes.Layers.list (null, null, 1)));
										}
									});
							}
						});
				}
			});
	}
	
	public static Promise<Result> list (final String query, final Boolean published, final long page) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);

		Logger.debug ("list Layers ");
		
		return from (database)
			.query (new ListLayers (page, query, published))
			.execute (new Function<Page<Layer>, Result> () {
				@Override
				public Result apply (final Page<Layer> layers) throws Throwable {
					Logger.debug ("Layer list : #" + layers.values().size());
					return ok (list.render (layers, query, published));
				}
			});
	}
	
	public static Promise<Result> listJson (final String query, final Boolean published, final long page) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);

		return from (database)
			.query (new ListLayers (page, query, published))
			.execute (new Function<Page<Layer>, Result> () {
				@Override
				public Result apply (final Page<Layer> layers) throws Throwable {
					final ObjectNode result = Json.newObject ();
					
					result.put ("header", layerPagerHeader.render (layers, query, published).toString ());
					result.put ("body", layerPagerBody.render (layers).toString ());
					result.put ("footer", layerPagerFooter.render (layers).toString ());

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
			.query (new ListStyles (1l, null))
			.query(new GetLayerServices(layerId))
			.executeFlat (new Function3<Layer, Page<Style>, List<String>, Promise<Result>> () {

				@Override
				public Promise<Result> apply (final Layer layer, final Page<Style> allStyles, final List<String> serviceIds) throws Throwable {
					String serviceId;
					if (serviceIds==null || serviceIds.isEmpty()){
						serviceId="";
					} else {
						Logger.debug ("Services for layer: " + layer.name() + " # " + serviceIds.size());								
						// get the first service in the list for preview
						serviceId=serviceIds.get(0);
					}
					return from (database)
							.get(Dataset.class, layer.datasetId())
							.get(Service.class, serviceId)
							.execute (new Function2<Dataset, Service, Result> () {

							@Override
							public Result apply (final Dataset dataset, final Service service) throws Throwable {
									
								LayerForm layerForm = new LayerForm (layer);
								layerForm.setKeywords(layer.getKeywords());
								
								List<Style> layerStyles ;
								if (layer.styles() == null){ 
									layerStyles = new ArrayList<Style>();
								} else {
									layerStyles = layer.styles(); 
								}
								layerForm.setStyleList(layerStyles);
									
								layerForm.setDatasetId(dataset.id());
								layerForm.setDatasetName(dataset.name());
								
								final Form<LayerForm> formLayerForm = Form
										.form (LayerForm.class)
										.fill (layerForm);
								
								Logger.debug ("Edit layerForm: " + layerForm);						
								Logger.debug ("allStyles: #" + allStyles.values().size());
								Logger.debug ("layerStyles: #" + layerStyles.size());
								
								// build a json string with list of styles (style.name, style.id) 
								final ArrayNode arrayNode = Json.newObject ().putArray ("styleList");
								for (final Style style: layerStyles) {
									final ArrayNode styleNode = arrayNode.addArray ();
									styleNode.add (style.name ());
									styleNode.add (style.id ());
								}					
								final String layerStyleListString = Json.stringify (arrayNode);
								
								// build a layer preview string
								final String previewUrl ;
								if (service==null){
									previewUrl = null;
								} else {
									previewUrl = makePreviewUrl(service.name(), layer.name());
								}
								return ok (form.render (formLayerForm, false, allStyles, layerStyleListString, previewUrl));
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
				return redirect (routes.Layers.list (null, null, 1));
			}
		});
		
	}
	
	
	public static class LayerForm extends TiledLayerForm{
		


		@Constraints.Required
		private String id;
		
		@Constraints.Required (message = "test")
		@Constraints.MinLength (value = 3, message = "web.application.page.services.form.field.name.validation.length")
		@Constraints.Pattern (value = "^[a-zA-Z][a-zA-Z0-9\\-\\_]+$", message = "web.application.page.layers.form.field.name.validation.error")
		private String name;
		
		private String title;
		private String abstractText;
		private List<String> keywords;
		private Boolean published = false;
		private String datasetId;
		private String datasetName;
		/**
		 * List of all styles in the system
		 */
		private List<Style> styleList;
		/**
		 * String that contains all styles of this layer in json format 
		 */
		private List<Style> styles;


		public LayerForm(){
			super();
			this.id = ID;
			this.keywords = new ArrayList<String>();
		}
		
		public LayerForm(Layer layer){
			this.id = layer.id();
			this.name = layer.name();
			this.title = layer.title();
			this.abstractText = layer.abstractText();
			this.published = layer.published();
			this.datasetId = layer.datasetId();
			this.datasetName = layer.datasetName();
			this.keywords = layer. getKeywords();
			this.styles = layer.styles();
			this.setTiledLayer(layer.tiledLayer().isPresent()?layer.tiledLayer().get():null);
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
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

		public Boolean getPublished() {
			return published;
		}

		public void setPublished(Boolean published) {
			this.published = published;
		}

		public List<Style> getStyleList() {
			return styleList;
		}

		public void setStyleList(List<Style> styleList) {
			this.styleList = styleList;
		}

		public List<Style> getStyles() {
			return styles;
		}

		public void setStyles(List<Style> styles) {
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

		@Override
		public String toString() {
			
			return "LayerForm [id=" + id + ", name=" + name + ", title=" + title + ", abstractText=" + abstractText
					+ ", keywords=" + keywords + ", published=" + published + ", styleList=" + styleList + ", styles="
					+ styles + "]";
		}

		
	}
}

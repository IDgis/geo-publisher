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
					if (form.field("styles").value().length() == 0 ) {
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
					final Layer layer = new Layer(layerForm.id, layerForm.name, layerForm.title, 
							layerForm.abstractText,layerForm.published,layerForm.datasetId, layerForm.datasetName,
							(layerForm.enabled ? layerForm.getTiledLayer() : null), layerForm.getKeywords(), layerForm.getStyleList());
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
		
//		@Constraints.Required (message = "test")
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
		 * List of styles in this layer
		 */
		private List<Style> styleList;
		/**
		 * Json array of all styles in the layer
		 */
		private String styles;
		private Boolean enabled = false;

		/*
		 * Tiled Layers
		 */
//		private static final Integer META_WIDTH_DEFAULT = 4;
//		private static final Integer META_HEIGTH_DEFAULT = 4;
//		private static final Integer EXPIRE_CACHE_DEFAULT = 0;
//		private static final Integer EXPIER_CLIENTS_DEFAULT = 0;
//		private static final Integer GUTTER_DEFAULT = 0;
//		private static final String GIF = "#gif#";
//		private static final String JPG = "#jpg#";
//		private static final String PNG8 = "#png8#";
//		private static final String PNG = "#png#";
//		private TiledLayer tiledLayer;
//		private Boolean png = false;
//		private Boolean png8 = false;
//		private Boolean jpg = false;
//		private Boolean gif = false;
//		private Integer metaWidth = META_WIDTH_DEFAULT;
//		private Integer metaHeight = META_HEIGTH_DEFAULT;
//		private Integer expireCache = EXPIRE_CACHE_DEFAULT;
//		private Integer expireClients = EXPIER_CLIENTS_DEFAULT;
//		private Integer gutter = GUTTER_DEFAULT;


		
		
		public LayerForm(){
			super();
			this.id = ID;
			this.keywords = new ArrayList<String>();
		}
		
		public LayerForm(Layer layer){
			this();
			this.id = layer.id();
			this.name = layer.name();
			this.title = layer.title();
			this.abstractText = layer.abstractText();
			this.published = layer.published();
			this.datasetId = layer.datasetId();
			this.datasetName = layer.datasetName();
			this.keywords = layer.getKeywords();
			this.styleList = layer.styles();
			this.setTiledLayer(layer.tiledLayer().isPresent()?layer.tiledLayer().get():null);
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

		public Boolean getPublished() {
			return published;
		}

		public void setPublished(Boolean published) {
			this.published = published;
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

//		public TiledLayer getTiledLayer() {
//			return new TiledLayer(getId(), getName(), getMetaWidth(), getMetaHeight(), getExpireCache(), getExpireClients(), getGutter(), getMimeFormats() );
//		}
//
//		public void setTiledLayer(TiledLayer tiledLayer) {
//			this.tiledLayer = tiledLayer;
//			if (tiledLayer == null){
//				this.metaWidth = META_WIDTH_DEFAULT;
//				this.metaHeight = META_HEIGTH_DEFAULT;
//				this.expireCache = EXPIRE_CACHE_DEFAULT;
//				this.expireClients = EXPIER_CLIENTS_DEFAULT;
//				this.gutter = GUTTER_DEFAULT;
//				setMimeFormats(null);
//			} else {
//				this.metaWidth = tiledLayer.metaWidth() == null ? META_WIDTH_DEFAULT : tiledLayer.metaWidth();
//				this.metaHeight = tiledLayer.metaHeight() == null ? META_HEIGTH_DEFAULT : tiledLayer.metaHeight();
//				this.expireCache = tiledLayer.expireCache() == null ? EXPIRE_CACHE_DEFAULT : tiledLayer.expireCache();
//				this.expireClients = tiledLayer.expireClients() == null ? EXPIER_CLIENTS_DEFAULT : tiledLayer.expireClients();
//				this.gutter = tiledLayer.gutter() == null ? GUTTER_DEFAULT : tiledLayer.gutter();
//				setMimeFormats(tiledLayer.mimeformats());
//			}
//		}
//
//
//		public List<String> getMimeFormats() {
//			List<String> mimeFormats = new ArrayList<String>();
//			if (getPng()) mimeFormats.add(PNG);
//			if (getPng8()) mimeFormats.add(PNG8);
//			if (getJpg()) mimeFormats.add(JPG);
//			if (getGif()) mimeFormats.add(GIF);
//			return mimeFormats;
//		}
//
//		public void setMimeFormats(List<String> mimeFormats) {
//			setPng(false);
//			setPng8(false);
//			setJpg(false);
//			setGif(false);
//			if (mimeFormats==null) return;
//			for (String string : mimeFormats) {
//				if (string.equals(PNG)) setPng(true);
//				if (string.equals(PNG8)) setPng8(true);
//				if (string.equals(JPG)) setJpg(true);
//				if (string.equals(GIF)) setGif(true);
//			}
//		}
//
//		public Boolean getPng() {
//			return png;
//		}
//
//		public void setPng(Boolean png) {
//			this.png = png;
//		}
//
//		public Boolean getPng8() {
//			return png8;
//		}
//
//		public void setPng8(Boolean png8) {
//			this.png8 = png8;
//		}
//
//		public Boolean getJpg() {
//			return jpg;
//		}
//
//		public void setJpg(Boolean jpg) {
//			this.jpg = jpg;
//		}
//
//		public Boolean getGif() {
//			return gif;
//		}
//
//		public void setGif(Boolean gif) {
//			this.gif = gif;
//		}
//
//		public Integer getMetaWidth() {
//			return metaWidth;
//		}
//
//		public void setMetaWidth(Integer metaWidth) {
//			this.metaWidth = metaWidth;
//		}
//
//		public Integer getMetaHeight() {
//			return metaHeight;
//		}
//
//		public void setMetaHeight(Integer metaHeight) {
//			this.metaHeight = metaHeight;
//		}
//
//		public Integer getExpireCache() {
//			return expireCache;
//		}
//
//		public void setExpireCache(Integer expireCache) {
//			this.expireCache = expireCache;
//		}
//
//		public Integer getExpireClients() {
//			return expireClients;
//		}
//
//		public void setExpireClients(Integer expireClients) {
//			this.expireClients = expireClients;
//		}
//
//		public Integer getGutter() {
//			return gutter;
//		}
//
//		public void setGutter(Integer gutter) {
//			this.gutter = gutter;
//		}
//
		
		@Override
		public String toString() {
			return "LayerForm [id=" + id + ", name=" + name + ", title=" + title + ", abstractText=" + abstractText
					+ ", keywords=" + keywords + ", published=" + published + ", datasetId=" + datasetId
					+ ", datasetName=" + datasetName + ", styleList=" + styles + ", enabled=" + enabled + ", toString()=" + super.toString() + "]";
		}
		
	}
}

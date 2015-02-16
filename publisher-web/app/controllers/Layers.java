package controllers;

import static models.Domain.from;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import models.Domain;
import models.Domain.Function;
import models.Domain.Function3;
import nl.idgis.publisher.domain.query.ListLayerStyles;
import nl.idgis.publisher.domain.query.PutLayerStyles;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.web.Dataset;
import nl.idgis.publisher.domain.web.Layer;
import nl.idgis.publisher.domain.web.Style;
import play.Logger;
import play.Play;
import play.data.Form;
import play.data.validation.Constraints;
import play.libs.Akka;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.layers.form;
import views.html.layers.list;
import actions.DefaultAuthenticator;
import akka.actor.ActorSelection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Security.Authenticated (DefaultAuthenticator.class)
public class Layers extends Controller {
	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");

	private static Promise<Result> renderCreateForm (final Form<LayerForm> layerForm) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		return from (database)
				.list (Style.class)
				.execute (new Function<Page<Style>, Result> () {

					@Override
					public Result apply (final Page<Style> allStyles) throws Throwable {
						Logger.debug ("allStyles: " + allStyles.values().size());
						Logger.debug ("layerStyles: " + layerForm.get().styles);
						return ok (form.render (layerForm, true, allStyles.values(), layerForm.get().styleList, ""));
					}
				});
	}
	
	public static Promise<Result> submitCreateUpdate () {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		final Form<LayerForm> form = Form.form (LayerForm.class).bindFromRequest ();
		Logger.debug ("submit Layer: " + form.field("name").value());
		Logger.debug ("LayerForm from request: " + form.get());	
		
		// validation
		if (form.field("name").value().length() == 1 ) 
			form.reject("name", Domain.message("web.application.page.layers.form.field.name.validation.error", "1"));
		Logger.debug ("LayerForm styles length: " + form.field("styles").value().length());	
		
		if (form.field("styles").value().length() == 0 ) 
			form.reject("styles", Domain.message("web.application.page.layers.form.field.styles.validation.error"));
		if (form.hasErrors ()) {
			return renderCreateForm (form);
		}
		
		// parse the list of (style.name, style.id) from the json string in the view form
		String layerStyleList = form.get().getStyles();
		final ObjectNode result = Json.newObject ();
		final JsonNode result2 = Json.parse(layerStyleList);
		
		final List<String> styleIds = new ArrayList<> ();
		for (final JsonNode n: Json.parse (layerStyleList)) {
			// get only the second element (style.id)
			styleIds.add (n.get (1).asText ());
		}
		Logger.debug ("layerStyleList: " + styleIds.toString ());
		
		final LayerForm layerForm = form.get ();
		final Layer layer = new Layer(layerForm.id, layerForm.name, layerForm.title, 
				layerForm.abstractText,layerForm.keywords,layerForm.published,layerForm.datasetName);
		
		return from (database)
				.put(layer)
				.executeFlat (new Function<Response<?>, Promise<Result>> () {
					@Override
					public Promise<Result> apply (final Response<?> response) throws Throwable {
						// Get the id of the layer we just put 
						String layerId = response.getValue().toString();
						PutLayerStyles putLayerStyles = new PutLayerStyles(layerId, styleIds);															
						return from (database)
							.query(putLayerStyles)
							.executeFlat (new Function<Response<?>, Promise<Result>> () {
								@Override
								public Promise<Result> apply (final Response<?> response) throws Throwable {
								
									if (CrudOperation.CREATE.equals (response.getOperation())) {
										Logger.debug ("Created layer " + layer);
										flash ("success", Domain.message("web.application.page.layers.name") + " " + layerForm.getName () + " is " + Domain.message("web.application.added").toLowerCase());
									}else{
										Logger.debug ("Updated layer " + layer);
										flash ("success", Domain.message("web.application.page.layers.name") + " " + layerForm.getName () + " is " + Domain.message("web.application.updated").toLowerCase());
									}
									return Promise.pure (redirect (routes.Layers.list ()));
								}
							});
					}
				});
	}
	
	public static Promise<Result> list () {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);

		Logger.debug ("list Layers ");
		
		return from (database)
			.list (Layer.class)
			.execute (new Function<Page<Layer>, Result> () {
				@Override
				public Result apply (final Page<Layer> layers) throws Throwable {
					Logger.debug ("Layer list #: " + layers.values().size());
					return ok (list.render (layers));
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
		// The list of styles for this layer is inititially empty
		layerForm.setStyleList(new ArrayList<Style>());
		
		return from (database)
				.get (Dataset.class, datasetId)
				.executeFlat (new Function<Dataset, Promise<Result>> () {
					@Override
					public Promise<Result> apply (final Dataset dataset) throws Throwable {
						Logger.debug ("dataset: " + dataset.name());
//						return ok (list.render (layers));
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
			.list (Style.class)
			.query(new ListLayerStyles(layerId))
			.execute (new Function3<Layer, Page<Style>, List<Style>, Result> () {

				@Override
				public Result apply (final Layer layer, final Page<Style> allStyles, final List<Style> layerStyles) throws Throwable {
					LayerForm layerForm = new LayerForm (layer);
					if (layerStyles==null){
						layerForm.setStyleList(new ArrayList<Style>());
					} else {
						layerForm.setStyleList(layerStyles);						
					}
					final Form<LayerForm> formLayerForm = Form
							.form (LayerForm.class)
							.fill (layerForm);
					
					Logger.debug ("Edit layerForm: " + layerForm);						

					Logger.debug ("allStyles: " + allStyles.values().size());
					Logger.debug ("layerStyles: " + layerStyles.size());
					
					// build a json string with list of styles (style.name, style.id) 
					final ArrayNode arrayNode = Json.newObject ().putArray ("styleList");
					for (final Style style: layerStyles) {
						final ArrayNode styleNode = arrayNode.addArray ();
						styleNode.add (style.name ());
						styleNode.add (style.id ());
					}					
					final String layerStyleListString = Json.stringify (arrayNode);
					
					return ok (form.render (formLayerForm, false, allStyles.values(), layerStyles, layerStyleListString));
				}
			});
	}

	public static Promise<Result> delete(final String layerId){
		Logger.debug ("delete Layer " + layerId);
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		from(database).delete(Layer.class, layerId)
		.execute(new Function<Response<?>, Result>() {
			
			@Override
			public Result apply(Response<?> a) throws Throwable {
				return redirect (routes.Layers.list ());
			}
		});
		
		return Promise.pure (redirect (routes.Layers.list ()));
	}
	
	
	public static class LayerForm {
		
		@Constraints.Required
		private String id;
		private String name;
		private String title;
		private String abstractText;
		private String keywords;
		private Boolean published = false;
		private String datasetName;
		/**
		 * List of all styles in the system
		 */
		private List<Style> styleList;
		/**
		 * String that contains all styles of this layer in json format 
		 */
		private String styles;

		public LayerForm(){
			super();
			this.id = UUID.randomUUID().toString();
		}
		
		public LayerForm(Layer layer){
			this.id = layer.id();
			this.name = layer.name();
			this.title = layer.title();
			this.abstractText = layer.abstractText();
			this.keywords = layer.keywords();
			this.published = layer.published();
			this.datasetName = layer.datasetName();

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

		public String getKeywords() {
			return keywords;
		}

		public void setKeywords(String keywords) {
			this.keywords = keywords;
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

		@Override
		public String toString() {
			
			return "LayerForm [id=" + id + ", name=" + name + ", title=" + title + ", abstractText=" + abstractText
					+ ", keywords=" + keywords + ", published=" + published + ", styleList=" + styleList + ", styles="
					+ styles + "]";
		}

		
	}
}
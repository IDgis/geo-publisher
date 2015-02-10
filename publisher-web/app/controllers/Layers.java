package controllers;

import static models.Domain.from;
import models.Domain;
import models.Domain.Function;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.web.Layer;
import play.Logger;
import play.Play;
import play.data.Form;
import play.data.validation.Constraints;
import play.libs.Akka;
import play.libs.F;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.layers.form;
import views.html.layers.list;
import actions.DefaultAuthenticator;
import akka.actor.ActorSelection;

@Security.Authenticated (DefaultAuthenticator.class)
public class Layers extends Controller {
	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");

	private static Promise<Result> renderCreateForm (final Form<LayerForm> layerForm) {
		 return Promise.promise(new F.Function0<Result>() {
             @Override
             public Result apply() throws Throwable {
                 return ok (form.render (layerForm, true));
             }
         });
	}
	
	public static Promise<Result> submitCreateUpdate () {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		final Form<LayerForm> form = Form.form (LayerForm.class).bindFromRequest ();
		Logger.debug ("submit Layer: " + form.field("name").value());
		
		// validation
		if (form.field("name").value().length() == 1 ) 
			form.reject("name", Domain.message("web.application.page.layers.form.field.name.validation.error", "1"));
		if (form.hasErrors ()) {
			return renderCreateForm (form);
		}
		
		final LayerForm layerForm = form.get ();
		final Layer layer = new Layer(layerForm.id, layerForm.name, layerForm.title, 
				layerForm.abstractText,layerForm.keywords,layerForm.published);
		
		return from (database)
			.put(layer)
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
	
	public static Promise<Result> list () {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);

		Logger.debug ("list Layers ");
		
		return from (database)
			.list (Layer.class)
			.execute (new Function<Page<Layer>, Result> () {
				@Override
				public Result apply (final Page<Layer> layers) throws Throwable {
					return ok (list.render (layers));
				}
			});
	}

	public static Promise<Result> create () {
		Logger.debug ("create Layer");
		final Form<LayerForm> layerForm = Form.form (LayerForm.class).fill (new LayerForm ());
		
		return renderCreateForm (layerForm);
	}
	
	public static Promise<Result> edit (final String layerId) {
		Logger.debug ("edit Layer: " + layerId);
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from (database)
			.get (Layer.class, layerId)
			.execute (new Function<Layer, Result> () {

				@Override
				public Result apply (final Layer layer) throws Throwable {
					final Form<LayerForm> layerForm = Form
							.form (LayerForm.class)
							.fill (new LayerForm (layer));
					
					Logger.debug ("Edit layerForm: " + layerForm);						

					return ok (form.render (layerForm, false));
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
		private Boolean published;

		public LayerForm(){
			super();
		}
		
		public LayerForm(Layer layer){
			this.id = layer.id();
			this.name = layer.name();
			this.title = layer.title();
			this.abstractText = layer.abstractText();
			this.keywords = layer.keywords();
			this.published = layer.published();

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
		
	}
}
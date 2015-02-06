package controllers;

import nl.idgis.publisher.domain.web.Layer;
import play.Logger;
import play.data.validation.Constraints;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.layers.form;
import views.html.layers.list;
import actions.DefaultAuthenticator;

@Security.Authenticated (DefaultAuthenticator.class)
public class Layers extends Controller {

	public static Promise<Result> createForm () {
		Logger.debug ("create Layer");
		
		return Promise.pure (ok (form.render ()));
	}
	
	public static Promise<Result> list () {
		Logger.debug ("list Layers ");
		
		return Promise.pure (ok (list.render ()));
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
	}
}
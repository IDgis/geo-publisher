package controllers;

import nl.idgis.publisher.domain.web.LayerGroup;
import play.Logger;
import play.data.validation.Constraints;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.groups.list;
import views.html.groups.form;
import actions.DefaultAuthenticator;

@Security.Authenticated (DefaultAuthenticator.class)
public class Groups extends Controller {

	// CRUD
	
	public static Promise<Result> createForm () {
		Logger.debug ("create Group");
		
		return Promise.pure (ok (form.render ()));
	}
	
	
	public static Promise<Result> list () {
		Logger.debug ("list Groups ");
		
		return Promise.pure (ok (list.render ()));
	}
	
	
	public static class GroupForm{

		@Constraints.Required
		private String id;
		private String name;
		private String title;
		private String abstractText;
		private Boolean published;

		public GroupForm(){
			super();
		}
		
		public GroupForm(LayerGroup group){
			this.id = group.id();
			this.name = group.name();
			this.title = group.title();
			this.abstractText = group.abstractText();
			this.published = group.published();

		}
	}
	
}
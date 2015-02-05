package controllers;

import play.Logger;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.layers.form;
import views.html.layers.list;
import actions.DefaultAuthenticator;

@Security.Authenticated (DefaultAuthenticator.class)
public class Layers extends Controller {

	// CRUD
	
	public static Promise<Result> createForm () {
		Logger.debug ("create Layer");
		
		return Promise.pure (ok (form.render ()));
	}
	
	public static Promise<Result> list () {
		Logger.debug ("list Layers ");
		
		return Promise.pure (ok (list.render ()));
	}
}
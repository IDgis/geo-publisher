package controllers;

import play.Logger;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.services.list;
import actions.DefaultAuthenticator;

@Security.Authenticated (DefaultAuthenticator.class)
public class Services extends Controller {

	// CRUD
	
	public static Promise<Result> list () {
		Logger.debug ("list Services ");
		
		return Promise.pure (ok (list.render ()));
	}
}
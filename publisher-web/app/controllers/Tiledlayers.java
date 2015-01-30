package controllers;

import play.Logger;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.tiledlayers.list;
import actions.DefaultAuthenticator;

@Security.Authenticated (DefaultAuthenticator.class)
public class Tiledlayers extends Controller {

	// CRUD
	
	public static Promise<Result> list () {
		Logger.debug ("list Tiled lagen ");
		
		return Promise.pure (ok (list.render ()));
	}
}
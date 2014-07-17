package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import views.html.login;

public class User extends Controller {

	public static Result login () {
		return ok (login.render ());
	}
}

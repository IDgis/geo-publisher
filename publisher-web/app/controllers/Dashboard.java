package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;

public class Dashboard extends Controller {

	public static Result index () {
        return ok(index.render("Your new application is ready."));
	}
}

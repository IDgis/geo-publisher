package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import views.html.datasets.list;
import views.html.datasets.form;

public class Datasets extends Controller {

	public static Result list () {
		return ok (list.render ());
	}
	
	public static Result createForm () {
		return ok (form.render ());
	}
}

package controllers;

import play.mvc.Controller;
import play.mvc.Result;

import views.html.logging.messages;
import views.html.logging.tasks;

public class Logging extends Controller {

	public static Result messages () {
		return ok (messages.render ());
	}
	
	public static Result tasks () {
		return ok (tasks.render ());
	}
}

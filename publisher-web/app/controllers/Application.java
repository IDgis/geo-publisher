package controllers;

import play.Routes;
import play.mvc.Controller;
import play.mvc.Result;

public class Application extends Controller {

	public static Result javascriptRoutes() {
		return ok(
			Routes.javascriptRouter("jsRoutes", 
				routes.javascript.DataSources.listByDataSourceAndCategoryJson(),
				routes.javascript.Datasets.listColumnsAction(),
				routes.javascript.Datasets.getDatasetJson (),
				routes.javascript.Datasets.scheduleRefresh ()
			)
		).as("text/javascript");
	}
}

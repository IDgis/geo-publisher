package controllers;

import play.Routes;
import play.mvc.Controller;
import play.mvc.Result;

public class Application extends Controller {

	public static Result javascriptRoutes() {
		return ok(
			Routes.javascriptRouter("jsRoutes", 
				routes.javascript.DataSources.listByDataSourceAndCategoryJson (),
				routes.javascript.Datasets.listColumnsAction (),
				routes.javascript.Datasets.getDatasetJson (),
				routes.javascript.Datasets.status (),
				routes.javascript.Datasets.scheduleRefresh (),
				routes.javascript.Events.events (),
				routes.javascript.Events.eventsWithTag (),
				routes.javascript.Styles.handleFileUploadRaw (),
				routes.javascript.Styles.validateSld (),
				routes.javascript.DataSources.refreshDatasources (),
				routes.javascript.DataSources.refreshDatasource (),
				routes.javascript.Layers.structureItem (),
				routes.javascript.Groups.structureItem (),
				routes.javascript.LdapUserGroups.cleanupUserGroups ()
			)
		).as("text/javascript");
	}
}

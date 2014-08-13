package controllers;

import static models.Domain.from;
import models.Domain.Function4;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.DashboardActiveTask;
import nl.idgis.publisher.domain.web.DashboardError;
import nl.idgis.publisher.domain.web.DashboardNotification;
import nl.idgis.publisher.domain.web.DataSource;
import play.Play;
import play.libs.Akka;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.index;
import actions.DefaultAuthenticator;
import actors.Database;
import akka.actor.ActorSelection;

@Security.Authenticated (DefaultAuthenticator.class)
public class Dashboard extends Controller {
	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");
	
	public static Promise<Result> index () {
		// Hack: force the database actor to be loaded:
		if (Database.instance == null) {
			throw new NullPointerException ();
		}
		
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from (database)
				.list (DataSource.class)
				.list(DashboardNotification.class)
				.list(DashboardActiveTask.class)
				.list(DashboardError.class)
				.execute (new Function4<Page<DataSource>, Page<DashboardNotification>, Page<DashboardActiveTask>, Page<DashboardError>, Result> () {
					@Override
					public Result apply (final Page<DataSource> dataSources, final Page<DashboardNotification> dashboardNotification, final Page<DashboardActiveTask> dashboardActiveTask, final Page<DashboardError> dashboardError) throws Throwable {
        			return ok(index.render (dataSources, dashboardNotification, dashboardActiveTask, dashboardError));
					}
				});

	}
}

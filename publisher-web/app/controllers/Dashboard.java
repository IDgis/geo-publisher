package controllers;

import static models.Domain.from;

import org.joda.time.LocalDateTime;

import models.Domain.Function4;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.query.ListIssues;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.ActiveTask;
import nl.idgis.publisher.domain.web.DataSource;
import nl.idgis.publisher.domain.web.Issue;
import nl.idgis.publisher.domain.web.Notification;
import play.Play;
import play.libs.Akka;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import views.html.index;
import actions.DefaultAuthenticator;
import actors.Database;
import akka.actor.ActorSelection;

@Security.Authenticated (DefaultAuthenticator.class)
public class Dashboard extends Controller {
	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");
	private final static int errorCount = Play.application ().configuration ().getInt ("publisher.admin.dashboard.errorCount", 10);
	
	public static Promise<Result> index () {
		// Hack: force the database actor to be loaded:
		if (Database.instance == null) {
			throw new NullPointerException ();
		}
		
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		final ListIssues listIssues;
		
		// Determine the last time errors have been displayed:
		final Http.Cookie messagesLastTime = request ().cookie ("messagesDisplayTime");
		if (messagesLastTime != null && !messagesLastTime.value ().isEmpty ()) {
			final long timestamp = Long.parseLong (messagesLastTime.value ());
			listIssues = new ListIssues (
					LogLevel.ERROR.andUp (), 
					new LocalDateTime (timestamp), 
					0l,
					(long)errorCount
				);
		} else {
			listIssues = new ListIssues (
					LogLevel.ERROR.andUp (),
					new LocalDateTime ().minusHours (12),
					0l,
					(long)errorCount
				);
		}
		
		return from (database)
				.list (DataSource.class)
				.list(Notification.class)
				.list(ActiveTask.class)
				.query (listIssues)
				.execute (new Function4<Page<DataSource>, Page<Notification>, Page<ActiveTask>, Page<Issue>, Result> () {
					@Override
					public Result apply (final Page<DataSource> dataSources, final Page<Notification> notifications, final Page<ActiveTask> activeTasks, final Page<Issue> issues) throws Throwable {
						return ok(index.render (dataSources, notifications, activeTasks, issues));
					}
				});

	}
}

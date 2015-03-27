package controllers;

import static models.Domain.from;

import java.sql.Timestamp;

import models.Domain.Function4;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.query.ListActiveNotifications;
import nl.idgis.publisher.domain.query.ListActiveTasks;
import nl.idgis.publisher.domain.query.ListIssues;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.ActiveTask;
import nl.idgis.publisher.domain.web.DataSource;
import nl.idgis.publisher.domain.web.Issue;
import nl.idgis.publisher.domain.web.Notification;
import nl.idgis.publisher.domain.web.DashboardItem;

import org.joda.time.DateTime;

import play.Logger;
import play.Play;
import play.libs.Akka;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import views.html.index;
import actions.DefaultAuthenticator;
import akka.actor.ActorSelection;

@Security.Authenticated (DefaultAuthenticator.class)
public class Dashboard extends Controller {
	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");
	private final static int errorCount = Play.application ().configuration ().getInt ("publisher.admin.dashboard.errorCount", 10);
	private final static int notificationCount = Play.application ().configuration ().getInt ("publisher.admin.dashboard.notificationCount", 10);
	
	public static Promise<Result> index () {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		final ListIssues listIssues;
		final ListActiveNotifications listNotifications;
		final ListActiveTasks listActiveTasks;
		
		// Determine the last time errors have been displayed:
		final Http.Cookie messagesLastTime = request ().cookie ("messagesDisplayTime");
		if (messagesLastTime != null && !messagesLastTime.value ().isEmpty ()) {
			final long timestamp = Long.parseLong (messagesLastTime.value ());
			Logger.debug ("Showing issues since: " + new Timestamp (timestamp));
			listIssues = new ListIssues (
					LogLevel.ERROR.andUp (), 
					new Timestamp (timestamp), 
					0l,
					(long)errorCount
				);
		} else {
			listIssues = new ListIssues (
					LogLevel.ERROR.andUp (),
					new Timestamp (new DateTime ().minusHours (12).getMillis ()),
					0l,
					(long)errorCount
				);
		}
		
		// List active notifications:
		listNotifications = new ListActiveNotifications (false, null, 0l, (long)notificationCount); 
		// List active tasks current and recent (last 24 h), only a few items (5) visible 
		listActiveTasks = new ListActiveTasks(new Timestamp(new java.util.Date().getTime() - (24*3600*1000)), 1L, 5L);
		
		return from (database)
			.list (DataSource.class)
			.query (listNotifications)
			.query (listActiveTasks)
			.query (listIssues)
			.execute (new Function4<Page<DataSource>, Page<Notification>, Page<ActiveTask>, Page<Issue>, Result> () {
				@Override
				public Result apply (
					final Page<DataSource> dataSources, 
					final Page<Notification> notifications, 
					final Page<ActiveTask> activeTasks, 
					final Page<Issue> issues) throws Throwable {
						return ok(index.render (dataSources, notifications, activeTasks, issues));
				}
			});

	}
}

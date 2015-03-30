package controllers;

import static models.Domain.from;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.Domain.Function;
import models.Domain.Function4;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.query.ListActiveTasks;
import nl.idgis.publisher.domain.query.ListIssues;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.ActiveTask;
import nl.idgis.publisher.domain.web.DataSource;
import nl.idgis.publisher.domain.web.Issue;
import nl.idgis.publisher.domain.web.Notification;
import play.Logger;
import play.Play;
import play.libs.Akka;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import actions.DefaultAuthenticator;
import akka.actor.ActorSelection;
import views.html.index;
import views.html.logging.messages;
import views.html.logging.tasks;

@Security.Authenticated (DefaultAuthenticator.class)
public class Logging extends Controller {
	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");

	public static Promise<Result> messages () {
		return messagesWithFilter ("", (long)1);
	}
	
	public static Promise<Result> messagesWithFilter (final String logLevelsList, final long page) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		// Determine log levels:
		final Set<LogLevel> logLevels = new HashSet<> ();
		
		for (final String part: logLevelsList.trim ().split (",")) {
			try {
				logLevels.add (LogLevel.valueOf (part.trim ().toUpperCase ()));
			} catch (IllegalArgumentException e) { }
		}

		// Fall back to the default set of log levels:
		if (logLevels.isEmpty ()) {
			logLevels.add (LogLevel.ERROR);
			logLevels.add (LogLevel.WARNING);
			logLevels.add (LogLevel.INFO);
		}
		
		return from (database)
			.query (new ListIssues (logLevels, null, page, 75l))
			.execute (new Function<Page<Issue>, Result> () {
				@Override
				public Result apply (final Page<Issue> issues) throws Throwable {
					// Store the last access time of this page:
					if (issues.currentPage () == 1 && logLevels.contains (LogLevel.ERROR)) {
						if (issues.values ().isEmpty()) {
							response ().discardCookie ("messageDisplayTime");
						} else {
							Logger.debug ("Storing last date: " + issues.values ().get (0).when ());
							response ().setCookie (
									"messagesDisplayTime",
									"" + issues.values ().get (0).when ().getTime ()
								);
						}
					}
					
					return ok (messages.render (issues, logLevels));
				}
			});
		
	}
	
	public static Promise<Result> tasks () {
		return tasksWithPaging(1L);
	}
	
	public static Promise<Result> tasksWithPaging (final long page) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		// List active tasks current and recent (last 24 h), one screen full with paging 
		ListActiveTasks listActiveTasks = 
			new ListActiveTasks(new Timestamp(new java.util.Date().getTime() - (24*3600*1000)), page, 10L);
		
		return from (database)
			.query (listActiveTasks)
			.execute (new Function<Page<ActiveTask>, Result> () {
				@Override
				public Result apply (final Page<ActiveTask> activeTasks) throws Throwable {
						return ok (tasks.render (activeTasks));
				}
			});
	}
	
	public static String logLevelsWith (final Set<LogLevel> logLevels, final LogLevel logLevel) {
		final Set<LogLevel> newSet = new HashSet<> (logLevels);
		
		newSet.add (logLevel);
		
		return logLevels (newSet);
	}
	
	public static String logLevelsWithout (final Set<LogLevel> logLevels, final LogLevel logLevel) {
		final Set<LogLevel> newSet = new HashSet<> (logLevels);
		
		newSet.remove (logLevel);
		
		return logLevels (newSet);
	}
	
	public static String logLevels (final Set<LogLevel> logLevels) {
		final List<String> logLevelsList = new ArrayList<> (logLevels.size ());
		
		for (final LogLevel logLevel: logLevels) {
			logLevelsList.add (logLevel.name ().toLowerCase ());
		}
		
		Collections.sort (logLevelsList);
		
		final StringBuilder builder = new StringBuilder ();
		
		for (final String logLevel: logLevelsList) {
			if (builder.length () > 0) {
				builder.append (",");
			}
			
			builder.append (logLevel);
		}
		
		return builder.toString ();
	}
}

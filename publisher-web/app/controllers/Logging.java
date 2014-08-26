package controllers;

import static models.Domain.from;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.Domain.Function;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.query.ListIssues;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Issue;
import play.Play;
import play.libs.Akka;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import akka.actor.ActorSelection;

import views.html.logging.messages;
import views.html.logging.tasks;

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
			.query (new ListIssues (logLevels, page))
			.execute (new Function<Page<Issue>, Result> () {
				@Override
				public Result apply (final Page<Issue> issues) throws Throwable {
					return ok (messages.render (issues, logLevels));
				}
			});
		
	}
	
	public static Result tasks () {
		return ok (tasks.render ());
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

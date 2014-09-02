package controllers;

import static models.Domain.from;

import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import models.Domain;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.query.ListActiveNotifications;
import nl.idgis.publisher.domain.query.ListIssues;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.ActiveTask;
import nl.idgis.publisher.domain.web.Issue;
import nl.idgis.publisher.domain.web.Notification;

import org.joda.time.DateTime;

import play.Play;
import play.libs.Akka;
import play.libs.F;
import play.libs.F.Callback;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import scala.concurrent.duration.Duration;
import views.html.helper.activeTasks;
import views.html.helper.activeTasksHeader;
import actions.JsonRestAuthenticator;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.SupervisorStrategy.Directive;
import akka.actor.UntypedActor;
import akka.japi.Function;
import akka.pattern.Patterns;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Security.Authenticated (JsonRestAuthenticator.class)
public class Events extends Controller {

	private final static ActorRef dispatcher = Akka.system ().actorOf (Props.create (Dispatcher.class), "event-dispatcher");
	private final static String defaultTag = "--";
	private final static int errorCount = Play.application ().configuration ().getInt ("publisher.admin.dashboard.errorCount", 10);
	private final static int notificationCount = Play.application ().configuration ().getInt ("publisher.admin.dashboard.notificationCount", 10);
	
	public static Promise<Result> events () {
		return eventsWithTag ("");
	}
	
	public static Promise<Result> eventsWithTag (final String tag) {
		
		final Http.Cookie messagesLastTime = request ().cookie ("messagesDisplayTime");
		final Long timestamp;
		if (messagesLastTime != null && !messagesLastTime.value ().isEmpty ()) {
			timestamp = Long.parseLong (messagesLastTime.value ());
		} else {
			timestamp = new Timestamp (new DateTime ().minusHours (12).getMillis ()).getTime ();
		}
		
		return Promise.wrap (Patterns.ask (dispatcher, new StartPoll (tag == null || tag.isEmpty () ? defaultTag : tag, timestamp), 25000))
			.recover (new F.Function<Throwable, Object> () {
				@Override
				public Object apply (final Throwable a) throws Throwable {
					final ObjectNode response = Json.newObject ();
					response.put ("tag", tag.isEmpty () ? defaultTag : tag);
					return response;
				}
			})
			.map (new F.Function<Object, Result>() {
				@Override
				public Result apply (final Object events) throws Throwable {
					if (events instanceof JsonNode) {
						return ok ((JsonNode) events);
					} else {
						throw new IllegalArgumentException ("Unknown message type");
					}
				}
			});
	}
	
	public static class Dispatcher extends UntypedActor {
		private final static SupervisorStrategy strategy = 
				new OneForOneStrategy(-1, Duration.Inf(), new Function<Throwable, Directive>() {
					@Override
					public Directive apply(Throwable t) {
						return OneForOneStrategy.stop();
					}
				});
		
		@Override
		public SupervisorStrategy supervisorStrategy () {
			return strategy;
		}
		
		@Override
		public void onReceive (final Object msg) throws Exception {
			if (msg instanceof StartPoll) {
				final StartPoll startPoll = (StartPoll) msg;
				final ActorRef ref = context ().actorOf (PollEventsActor.mkProps (sender (), startPoll.tag, startPoll.messagesLastTime));
				ref.tell (Event.UPDATE, sender ());
			} else {
				unhandled (msg);
			}
		}
	}
	
	public static class PollEventsActor extends UntypedActor {
		private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");
		private final ActorRef target;
		private final String[] tags;
		private final Timestamp messagesLastTime;
		
		public PollEventsActor (final ActorRef target, final String tag, final long messagesLastTime) {
			final String[] parts = tag.split ("\\-");
			
			this.target = target;
			this.tags = new String[] {
				parts.length > 0 ? parts[0] : "",
				parts.length > 1 ? parts[1] : "",
				parts.length > 2 ? parts[2] : ""
			};
			this.messagesLastTime = new Timestamp (messagesLastTime);
		}
		
		public static Props mkProps (final ActorRef target, final String tag, final long messagesLastTime) {
			return Props.create (PollEventsActor.class, target, tag, messagesLastTime);
		}
		
		@Override
		public void preStart () {
			final ActorRef self = self ();

			// Schedule destruction of this actor:
			context ().system ().scheduler ().scheduleOnce (
				Duration.create (25, TimeUnit.SECONDS), 
				new Runnable () {
					@Override
					public void run () {
						self.tell (Event.STOP, self);
					}
				},
				context ().dispatcher ());
		}
		
		private void scheduleUpdate (final ActorRef self) {
			context ().system ().scheduler ().scheduleOnce (
					Duration.create (2, TimeUnit.SECONDS), 
					new Runnable () {
						@Override
						public void run () {
							self.tell (Event.UPDATE, self);
						}
					}, 
					context ().dispatcher ());
		}
		
		private void doUpdate () {
			final ActorRef self = self ();
			final ActorSelection database = Akka.system().actorSelection (databaseRef);

			final ListIssues listIssues;
			final ListActiveNotifications listNotifications;
			
			// List active issues:
			listIssues = new ListIssues (
					LogLevel.ERROR.andUp (), 
					messagesLastTime, 
					0l,
					(long)errorCount
				);
			
			// List active notifications:
			listNotifications = new ListActiveNotifications (false, null, 0l, (long)notificationCount);
			
			final Promise<EventInfo> eventInfo = from (database)
				.list (ActiveTask.class)
				.query (listNotifications)
				.query (listIssues)
				.execute (new Domain.Function3<Page<ActiveTask>, Page<Notification>, Page<Issue>, EventInfo> () {
					@Override
					public EventInfo apply (final Page<ActiveTask> tasks, final Page<Notification> notifications, final Page<Issue> issues) throws Exception {
						return new EventInfo (tasks, notifications, issues);
					}
				});
			
			eventInfo.onRedeem (new Callback<EventInfo> () {
				@Override
				public void invoke (final EventInfo eventInfo) throws Throwable {
					final TaggedResponse tasks = emitTasks (tags[0], eventInfo.activeTasks);
					final TaggedResponse notifications = emitNotifications (tags[1], eventInfo.notifications);
					final TaggedResponse issues = emitIssues (tags[2], eventInfo.issues);
					
					if (tasks != null || notifications != null || issues != null) {
						final ObjectNode response = Json.newObject ();
						final String[] newTag = new String[3];
						
						if (tasks != null) {
							response.put ("activeTasks", tasks.node);
							newTag[0] = tasks.tag;
						} else {
							newTag[0] = tags[0];
						}
						if (notifications != null) {
							response.put ("notifications", notifications.node);
							newTag[1] = notifications.tag;
						} else {
							newTag[1] = tags[1];
						}
						if (issues != null) {
							response.put ("issues", issues.node);
							newTag[2] = issues.tag;
						} else {
							newTag[2] = tags[2];
						}
						
						response.put ("tag", String.format ("%s-%s-%s", newTag[0], newTag[1], newTag[2]));
						
						target.tell (response, self);
						self.tell (Event.STOP, self);
					} else {
						scheduleUpdate (self);
					}
				}
			});
		}
		
		private static String md5 (final String input) throws Throwable {
			final MessageDigest md = MessageDigest.getInstance ("MD5");
			
			md.update (input.getBytes ());
			
			return bytesToHex (md.digest ());
		}
		
		private static TaggedResponse emitTasks (final String currentTag, final Page<ActiveTask> tasks) throws Throwable {
			final JsonNode tasksNode = Json.toJson (tasks.values ());
			final String newTag = md5 (Json.stringify (tasksNode));
			
			if (!newTag.equals (currentTag)) {
				final ObjectNode response = Json.newObject ();
				
				response.put ("list", tasksNode);
				response.put ("content", activeTasks.render (tasks).body ());
				response.put ("headerContent", activeTasksHeader.render (tasks).body ());
				response.put ("hasMore", tasks.hasMorePages ());
				
				return new TaggedResponse (newTag, response);
			}

			return null;
		}
		
		private static TaggedResponse emitNotifications (final String currentTag, final Page<Notification> notifications) throws Throwable {
			final JsonNode notificationsNode = Json.toJson (notifications.values ());
			final String newTag = md5 (Json.stringify (notificationsNode));
			
			if (!newTag.equals (currentTag)) {
				final ObjectNode response = Json.newObject ();
				
				response.put ("list", notificationsNode);
				response.put ("content", views.html.helper.notifications.render (notifications).body ());
				response.put ("headerContent", views.html.helper.notificationsHeader.render (notifications).body ());
				response.put ("hasMore", notifications.hasMorePages ());
				
				return new TaggedResponse (newTag, response);
			}
			
			return null;
		}
		
		private static TaggedResponse emitIssues (final String currentTag, final Page<Issue> issues) throws Throwable {
			final JsonNode issuesNode = Json.toJson (issues.values ());
			final String newTag = md5 (Json.stringify (issuesNode));
			
			if (!newTag.equals (currentTag)) {
				final ObjectNode response = Json.newObject ();
				
				response.put ("list", issuesNode);
				response.put ("content", views.html.helper.issues.render (issues).body ());
				response.put ("headerContent", views.html.helper.issuesHeader.render (issues).body ());
				response.put ("hasMore", issues.hasMorePages ());
				
				return new TaggedResponse (newTag, response);
			}
			
			return null;
		}
		
		private static String bytesToHex(byte[] b) {
			char hexDigit[] = {'0', '1', '2', '3', '4', '5', '6', '7',
					'8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
			StringBuffer buf = new StringBuffer();
			for (int j=0; j<b.length; j++) {
				buf.append(hexDigit[(b[j] >> 4) & 0x0f]);
				buf.append(hexDigit[b[j] & 0x0f]);
			}
			return buf.toString();
		}
		
		@Override
		public void onReceive (final Object msg) throws Exception {
			if (Event.UPDATE.equals (msg)) {
				doUpdate ();
			} else if (Event.STOP.equals (msg)) {
				context ().stop (self ());
			} else {
				unhandled (msg);
			}
		}
	}
	
	public static enum Event {
		UPDATE,
		STOP
	}

	public final static class StartPoll {
		public final String tag;
		public final long messagesLastTime;
		
		public StartPoll (final String tag, final long messagesLastTime) {
			this.tag = tag;
			this.messagesLastTime = messagesLastTime;
		}
	}
	
	private final static class EventInfo {
		public final Page<ActiveTask> activeTasks;
		public final Page<Notification> notifications;
		public final Page<Issue> issues;
		
		public EventInfo (final Page<ActiveTask> activeTasks, final Page<Notification> notifications, final Page<Issue> issues) {
			this.activeTasks = activeTasks;
			this.notifications = notifications;
			this.issues = issues;
		}
	}
	
	private final static class TaggedResponse {
		public final String tag;
		public final JsonNode node;
		
		public TaggedResponse (final String tag, final JsonNode node) {
			this.tag = tag;
			this.node = node;
		}
	}
}

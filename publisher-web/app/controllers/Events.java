package controllers;

import static models.Domain.from;

import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

import models.Domain;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.ActiveTask;
import play.Logger;
import play.Play;
import play.libs.Akka;
import play.libs.F;
import play.libs.F.Callback;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import scala.concurrent.duration.Duration;
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

import views.html.helper.activeTasks;
import views.html.helper.activeTasksHeader;

public class Events extends Controller {

	private final static ActorRef dispatcher = Akka.system ().actorOf (Props.create (Dispatcher.class), "event-dispatcher");
	private final static String defaultTag = "-";
	
	public static Promise<Result> events () {
		return eventsWithTag ("");
	}
	
	public static Promise<Result> eventsWithTag (final String tag) {
		
		return Promise.wrap (Patterns.ask (dispatcher, tag == null || tag.isEmpty ()? defaultTag : tag, 25000))
			.recover (new F.Function<Throwable, Object> () {
				@Override
				public Object apply (final Throwable a) throws Throwable {
					Logger.debug ("Timeout during polling");
					
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
			if (msg instanceof String) {
				final ActorRef ref = context ().actorOf (PollEventsActor.mkProps (sender (), (String)msg));
				ref.tell (Event.UPDATE, sender ());
			} else {
				unhandled (msg);
			}
		}
	}
	
	public static class PollEventsActor extends UntypedActor {
		private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");
		private final ActorRef target;
		private final String tag;
		
		public PollEventsActor (final ActorRef target, final String tag) {
			this.target = target;
			this.tag = tag;
		}
		
		public static Props mkProps (final ActorRef target, final String tag) {
			return Props.create (PollEventsActor.class, target, tag);
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
			
			Logger.debug ("Polling for event updates: " + tag + ", " + self ().path ());
			
			final ActorSelection database = Akka.system().actorSelection (databaseRef);

			final Promise<Page<ActiveTask>> page = from (database)
				.list (ActiveTask.class)
				.execute (new Domain.Function<Page<ActiveTask>, Page<ActiveTask>> () {
					@Override
					public Page<ActiveTask> apply (final Page<ActiveTask> tasks) throws Exception {
						return tasks;
					}
				});
			
			page.onRedeem (new Callback<Page<ActiveTask>> () {
				@Override
				public void invoke (final Page<ActiveTask> tasks) throws Throwable {
					final JsonNode tasksNode = Json.toJson (tasks.values ());
					final String jsonString = Json.stringify (tasksNode);
					final MessageDigest md = MessageDigest.getInstance ("MD5");
					
					md.update (jsonString.getBytes ());
					
					final String newTag = bytesToHex (md.digest ());
					
					if (!newTag.equals (tag)) {
						final ObjectNode response = Json.newObject ();
						final ObjectNode tasksResponse = Json.newObject ();
						
						tasksResponse.put ("list", tasksNode);
						tasksResponse.put ("content", activeTasks.render (tasks).body ());
						tasksResponse.put ("headerContent", activeTasksHeader.render (tasks).body ());
						response.put ("tag", newTag);
						response.put ("activeTasks", tasksResponse);
						
						target.tell (response, self);
						self.tell (Event.STOP, self);
					} else {
						scheduleUpdate (self);
					}
				}
			});
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
				Logger.debug ("Terminating event actor: " + self ().path ());
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
}

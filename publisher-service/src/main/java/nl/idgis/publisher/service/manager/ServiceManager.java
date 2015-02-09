package nl.idgis.publisher.service.manager;

import java.util.Collections;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncDatabaseHelper;

import nl.idgis.publisher.service.manager.messages.DatasetNode;
import nl.idgis.publisher.service.manager.messages.DefaultService;
import nl.idgis.publisher.service.manager.messages.GetService;
import nl.idgis.publisher.service.manager.messages.GroupNode;
import nl.idgis.publisher.utils.FutureUtils;

public class ServiceManager extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database;
	
	private FutureUtils f;
	
	private AsyncDatabaseHelper db;
	
	public ServiceManager(ActorRef database) {
		this.database = database;
	}
	
	public static Props props(ActorRef database) {
		return Props.create(ServiceManager.class, database);
	}
	
	@Override
	public void preStart() throws Exception {
		f = new FutureUtils(getContext().dispatcher());
		db = new AsyncDatabaseHelper(database, f, log);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof GetService) {
			handleGetService((GetService)msg);
		} else {
			unhandled(msg);
		}
	}

	private void handleGetService(GetService msg) {
		ActorRef sender = getSender(), self = getSelf();
		
		GroupNode root = new GroupNode("group0", "name0", "title0", "abstract0");
		sender.tell(new DefaultService("service0", root, Collections.<DatasetNode>emptyList(), Collections.singletonList(root), Collections.<String, String>emptyMap()), self);
	}
}

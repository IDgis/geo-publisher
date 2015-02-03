package nl.idgis.publisher.protocol;

import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.GetMessagePackager;
import nl.idgis.publisher.protocol.messages.SetPersistent;
import nl.idgis.publisher.protocol.messages.StopPackager;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class MessagePackagerProvider extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final String pathPrefix;
	private final ActorRef messageTarget;
	
	private BiMap<String, ActorRef> messagePackagers;
	private Set<ActorRef> persistentPackagers;
	
	private MessagePackagerProvider(ActorRef messageTarget, String pathPrefix) {
		this.messageTarget = messageTarget;
		this.pathPrefix = pathPrefix;
	}
	
	public static Props props(ActorRef messageTarget, String pathPrefix) {
		return Props.create(MessagePackagerProvider.class, messageTarget, pathPrefix);
	}
	
	@Override
	public void preStart() {
		messagePackagers = HashBiMap.create();
		persistentPackagers = new HashSet<>();
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof GetMessagePackager) {
			GetMessagePackager gmp = (GetMessagePackager)msg;
			String targetName = gmp.getTargetName();
			
			log.debug("message packager requested for: " + targetName);
			
			final ActorRef packager;			
			if(messagePackagers.containsKey(targetName)) {
				log.debug("existing packager found");
				packager = messagePackagers.get(targetName);
			} else {
				log.debug("creating new packager");				
				packager = getContext().actorOf(MessagePackager.props(targetName, messageTarget, pathPrefix), URLEncoder.encode(targetName, "utf-8"));
				getContext().watch(packager);
				messagePackagers.put(targetName, packager);
			}
			
			if(gmp.getPersistent()) {
				if(persistentPackagers.add(packager)) {
					log.debug("packager marked as persistent");
				}
			}
			
			getSender().tell(packager, getSelf());
		} else if(msg instanceof Terminated) {
			ActorRef packager = ((Terminated) msg).getActor();
			String targetName = messagePackagers.inverse().remove(packager);
			
			if(targetName == null) {
				throw new IllegalStateException("Couldn't find packager in map");
			}
			
			log.debug("packager for target '" + targetName + "' terminated");			
		} else if(msg instanceof StopPackager) {			
			String targetName = ((StopPackager) msg).getTargetName();			
			
			log.debug("stop requested: " + targetName);
			if(messagePackagers.containsKey(targetName)) {
				ActorRef packager = messagePackagers.get(targetName);
				if(persistentPackagers.contains(packager)) {
					log.error("stop requested for persistent packager for target '" + targetName + "' -> not stopping");
				} else {
					log.debug("stopping packager");
					getContext().stop(packager);
				}
			} else {
				log.warning("no packager for target");
			}
			
			ActorRef sender = getSender();
			if(messagePackagers.containsValue(sender)) {
				log.debug("stop packager request send by a packager -> not acknowledging");
			} else {
				log.debug("acknowledge stop packager request");
				
				sender.tell(new Ack(), getSelf());
			}
			
		} else if(msg instanceof SetPersistent) {
			log.debug("set persistent");
			
			if(messagePackagers.containsValue(getSender())) {
				log.debug("packager requested to be marked as persistent");
				
				persistentPackagers.add(getSender());
			} else {
				log.error("SetPersistent sender is not a known packager");
			}
		} else {
			unhandled(msg);
		}
	}
}

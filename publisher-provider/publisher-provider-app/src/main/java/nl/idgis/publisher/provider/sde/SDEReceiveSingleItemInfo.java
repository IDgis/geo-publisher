package nl.idgis.publisher.provider.sde;

import java.util.Optional;

import nl.idgis.publisher.provider.protocol.Records;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.stream.messages.Stop;
import nl.idgis.publisher.stream.messages.Unavailable;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class SDEReceiveSingleItemInfo extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef target;
	
	private Object content;
	
	public SDEReceiveSingleItemInfo(ActorRef target) {
		this.target = target;
	}
	
	public static Props props(ActorRef target) {
		return Props.create(SDEReceiveSingleItemInfo.class, target);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Item) {
			log.debug("item");
			
			if(content == null) {
				content = ((Item<?>) msg).getContent();
				getSender().tell(new NextItem(), getSelf());
			} else {
				getSender().tell(new Stop(), getSelf());
				target.tell(new Unavailable(), getSelf());
				getContext().stop(getSelf());
			}
		} else if(msg instanceof End) {
			log.debug("end");
			
			target.tell(
				Optional.ofNullable(content)
					.filter(Records.class::isInstance)
					.map(Records.class::cast)
					.map(SDEUtils::toItemInfo)
					.map(Object.class::cast)
					.orElse(new Unavailable()),
				getSelf());
			getContext().stop(getSelf());
		} else {
			unhandled(msg);
		}
	}

}

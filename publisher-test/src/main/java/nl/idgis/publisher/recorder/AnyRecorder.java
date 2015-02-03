package nl.idgis.publisher.recorder;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import nl.idgis.publisher.recorder.messages.Create;
import nl.idgis.publisher.recorder.messages.Created;
import nl.idgis.publisher.recorder.messages.RecordedMessage;
import nl.idgis.publisher.recorder.messages.RecorderCommand;
import nl.idgis.publisher.recorder.messages.Watch;
import nl.idgis.publisher.recorder.messages.Watching;

public class AnyRecorder extends UntypedActor {
	
	private ActorRef recorder;
	
	public static Props props() {
		return Props.create(AnyRecorder.class);
	}
	
	@Override
	public void preStart() throws Exception {
		recorder = getContext().actorOf(Recorder.props());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof RecorderCommand) {
			recorder.forward(msg, getContext());
		} else if(msg instanceof Watch) {
			getContext().watch(((Watch)msg).getActorRef());
			getSender().tell(new Watching(), getSelf());
		} else if(msg instanceof Create) {
			ActorRef actorRef = getContext().actorOf(((Create)msg).getProps());
			getSender().tell(new Created(actorRef), getSelf());
		} else {
			recorder.tell(new RecordedMessage(getSelf(), getSender(), msg), getSelf());
			onRecord(msg, getSender());
		}
	}
	
	protected void onRecord(Object msg, ActorRef sender) {
		
	}

}

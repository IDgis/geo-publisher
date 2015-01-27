package nl.idgis.publisher.recorder;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import nl.idgis.publisher.recorder.messages.Clear;
import nl.idgis.publisher.recorder.messages.GetRecording;
import nl.idgis.publisher.recorder.messages.RecordedMessage;

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
		if(msg instanceof GetRecording || msg instanceof Clear) {
			recorder.forward(msg, getContext());
		} else {
			recorder.tell(new RecordedMessage(getSelf(), getSender(), msg), getSelf());
			onRecord(msg, getSender());
		}
	}
	
	protected void onRecord(Object msg, ActorRef sender) {
		
	}

}

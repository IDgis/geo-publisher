package nl.idgis.publisher.recorder;

import java.util.ArrayList;
import java.util.List;

import akka.actor.Props;
import akka.actor.UntypedActor;

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.recorder.messages.Clear;
import nl.idgis.publisher.recorder.messages.GetRecording;
import nl.idgis.publisher.recorder.messages.RecordedMessage;

public class Recorder extends UntypedActor {
	
	private final List<RecordedMessage> recording = new ArrayList<RecordedMessage>();
	
	public static Props props() {
		return Props.create(Recorder.class);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof RecordedMessage) {
			recording.add((RecordedMessage)msg);
		} else if(msg instanceof Clear) {
			recording.clear();
			getSender().tell(new Ack(), getSelf());
		} else if(msg instanceof GetRecording) {
			getSender().tell(new ArrayList<>(recording), getSelf());
		} else {
			unhandled(msg);
		}
		
	}

	
}

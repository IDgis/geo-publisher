package nl.idgis.publisher.provider;

import java.util.ArrayList;
import java.util.List;

import nl.idgis.publisher.provider.messages.GetRecording;
import nl.idgis.publisher.provider.messages.Record;

import akka.actor.Props;
import akka.actor.UntypedActor;

public class Recorder extends UntypedActor {
	
	private final List<Record> recording = new ArrayList<Record>();
	
	public static Props props() {
		return Props.create(Recorder.class);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Record) {
			recording.add((Record)msg);
		} else if(msg instanceof GetRecording) {
			getSender().tell(new ArrayList<>(recording), getSelf());
		} else {
			unhandled(msg);
		}
		
	}

	
}

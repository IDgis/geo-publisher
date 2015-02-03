package nl.idgis.publisher.recorder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import nl.idgis.publisher.recorder.messages.Clear;
import nl.idgis.publisher.recorder.messages.Cleared;
import nl.idgis.publisher.recorder.messages.GetRecording;
import nl.idgis.publisher.recorder.messages.RecordedMessage;
import nl.idgis.publisher.recorder.messages.Wait;
import nl.idgis.publisher.recorder.messages.Waited;

public class Recorder extends UntypedActor {
	
	private final List<RecordedMessage> recording = new ArrayList<RecordedMessage>();
	
	private Map<Integer, List<ActorRef>> waiting;
	
	public static Props props() {
		return Props.create(Recorder.class);
	}
	
	@Override
	public void preStart() throws Exception {
		waiting = new HashMap<>();
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof RecordedMessage) {
			recording.add((RecordedMessage)msg);
			
			int size = recording.size();
			if(waiting.containsKey(size)) {
				for(ActorRef actorRef : waiting.get(size)) {
					actorRef.tell(new Waited(), getSender());
				}
				
				waiting.remove(size);
			}
		} else if(msg instanceof Clear) {
			recording.clear();
			getSender().tell(new Cleared(), getSelf());
		} else if(msg instanceof Wait) {
			int count = ((Wait)msg).getCount();
			
			if(count - recording.size() > 0) {
				if(!waiting.containsKey(count)) {
					waiting.put(count, new ArrayList<>());
				}
				
				waiting.get(count).add(getSender());
			} else {
				getSender().tell(new Waited(), getSelf());
			}
		} else if(msg instanceof GetRecording) {
			getSender().tell(new DefaultRecording(new ArrayList<>(recording).iterator()), getSelf());
		} else {
			unhandled(msg);
		}
		
	}

	
}

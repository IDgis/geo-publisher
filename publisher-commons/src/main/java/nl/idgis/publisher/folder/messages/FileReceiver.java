package nl.idgis.publisher.folder.messages;

import java.io.Serializable;

import akka.actor.ActorRef;

public class FileReceiver implements Serializable {

	private static final long serialVersionUID = 3621779900124510071L;
	
	private final ActorRef receiver;

	public FileReceiver(ActorRef receiver) {
		this.receiver = receiver;
	}

	public ActorRef getReceiver() {
		return receiver;
	}

	@Override
	public String toString() {
		return "FileReceiver [receiver=" + receiver + "]";
	}
}

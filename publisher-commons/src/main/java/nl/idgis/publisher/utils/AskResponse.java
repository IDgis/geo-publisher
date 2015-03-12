package nl.idgis.publisher.utils;

import java.io.Serializable;

import akka.actor.ActorRef;

public class AskResponse<T> implements Serializable {

	private static final long serialVersionUID = -2838932142054936244L;

	private final T msg;
	
	private ActorRef sender;
	
	public AskResponse(T msg, ActorRef sender) {
		this.msg = msg;
		this.sender = sender;
	}
	
	public T getMessage() {
		return msg;
	}
	
	public ActorRef getSender() {
		return sender;
	}

	@Override
	public String toString() {
		return "AskResponse [msg=" + msg + ", sender=" + sender + "]";
	}
}

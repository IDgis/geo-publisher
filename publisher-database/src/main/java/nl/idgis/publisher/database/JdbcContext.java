package nl.idgis.publisher.database;

import java.sql.Connection;

import nl.idgis.publisher.protocol.messages.Ack;
import akka.actor.ActorRef;

class JdbcContext {
	
	private boolean answered = false;
	
	private final ActorRef sender, self;
	private final Connection connection;
	
	JdbcContext(Connection connection, ActorRef sender, ActorRef self) {
		this.connection = connection;
		this.sender = sender;
		this.self = self;
	}

	Connection getConnection() {
		return this.connection;
	}
	
	void answer(Object msg) {
		if(answered) {
			throw new IllegalArgumentException("query already answered");
		} else {
			sender.tell(msg, self);
			answered = true;
		}
	}
	
	void ack() {
		answer(new Ack());
	}
	
	void finish() {
		if(!answered) {
			throw new IllegalStateException("query not answered");
		}
	}
}

package nl.idgis.publisher.database;

import java.sql.Connection;

import akka.actor.ActorRef;

class JdbcContext {
	
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
		sender.tell(msg, self);
	}
}

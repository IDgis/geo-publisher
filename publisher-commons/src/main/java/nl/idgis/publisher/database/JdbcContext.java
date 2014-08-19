package nl.idgis.publisher.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import nl.idgis.publisher.database.messages.NotFound;
import nl.idgis.publisher.protocol.messages.Ack;

import akka.actor.ActorRef;
import akka.japi.Function;

public class JdbcContext {
	
	private boolean answered = false;
	
	private final ActorRef sender, self;
	private final Connection connection;
	
	public static class Prepared {
		
		PreparedStatement stmt;
		
		private Prepared(PreparedStatement stmt) {
			this.stmt = stmt;
		}
		
		public void execute(Object... args) throws Exception {
			execute(Arrays.asList(args));
		}		
		
		public void execute(List<Object> args) throws Exception {
			execute(args, new Function<Object, Object>() {

				@Override
				public Object apply(Object o) throws Exception {
					return o;
				}				
			});
		}
		
		public void execute(List<Object> args, Function<Object, Object> converter) throws Exception {
			int i = 1;
			
			for(Object arg : args) {
				stmt.setObject(i++, converter.apply(arg));
			}
			
			stmt.execute();
			stmt.close();
		}
	}
	
	JdbcContext(Connection connection, ActorRef sender, ActorRef self) {
		this.connection = connection;
		this.sender = sender;
		this.self = self;
	}

	public Connection getConnection() {
		return this.connection;
	}
	
	public void answer(Object msg) {
		if(answered) {
			throw new IllegalArgumentException("query already answered");
		} else {			
			sender.tell(msg == null ? new NotFound() : msg, self);
			answered = true;
		}
	}
	
	public void execute(String sql) throws SQLException {
		Statement stmt = connection.createStatement();
		stmt.execute(sql);
		stmt.close();
	}
	
	public Prepared prepare(String sql) throws SQLException {
		return new Prepared(connection.prepareStatement(sql));
	}
	
	public void ack() {
		answer(new Ack());
	}
	
	void finish() {
		if(!answered) {
			throw new IllegalStateException("query not answered");
		}
	}
}

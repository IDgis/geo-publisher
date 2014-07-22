package nl.idgis.publisher.provider.messages;

import java.io.Serializable;

import akka.io.Tcp.CommandFailed;

public class ConnectFailed implements Serializable {
	
	private static final long serialVersionUID = -6906852544207043716L;
	
	private final CommandFailed cause;	
	
	public ConnectFailed(CommandFailed cause) {
		this.cause = cause;
	}
	
	public CommandFailed getCause() {
		return cause;
	}

	@Override
	public String toString() {
		return "ConnectFailed [cause=" + cause + "]";
	}
}

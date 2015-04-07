package nl.idgis.publisher.harvester.sources.messages;

import java.io.Serializable;

import akka.actor.Props;

public abstract class FetchDataset implements Serializable {

	private static final long serialVersionUID = -7741526664260819770L;
	
	protected final String id;
	
	protected final Props receiverProps;
	
	protected FetchDataset(String id, Props receiverProps) {
		this.id = id;
		this.receiverProps = receiverProps;
	}
	
	public String getId() {
		return id;
	}
	
	public Props getReceiverProps() {
		return receiverProps;
	}

}

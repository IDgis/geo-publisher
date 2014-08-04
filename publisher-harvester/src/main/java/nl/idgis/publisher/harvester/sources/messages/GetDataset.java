package nl.idgis.publisher.harvester.sources.messages;

import java.io.Serializable;

import akka.actor.Props;

public class GetDataset implements Serializable {

	private static final long serialVersionUID = 4532518686754843432L;
	
	private final String id;
	private final Props receiverProps;
	
	public GetDataset(String id, Props receiverProps) {
		this.id = id;
		this.receiverProps = receiverProps;
	}

	public String getId() {
		return id;
	}	
	
	public Props getReceiverProps() {
		return receiverProps;
	}

	@Override
	public String toString() {
		return "GetDataset [id=" + id + ", receiverProps=" + receiverProps
				+ "]";
	}
	
}

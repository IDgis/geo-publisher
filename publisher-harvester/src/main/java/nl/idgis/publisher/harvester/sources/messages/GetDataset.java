package nl.idgis.publisher.harvester.sources.messages;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import akka.actor.Props;

public class GetDataset implements Serializable {
	
	private static final long serialVersionUID = -4313308340786549531L;
	
	private final String id;
	private final List<String> columns;
	private final Props receiverProps;
	
	public GetDataset(String id, List<String> columns, Props receiverProps) {
		this.id = id;		
		this.columns = columns;
		this.receiverProps = receiverProps;
	}

	public String getId() {
		return id;
	}	
	
	public List<String> getColumns() {
		return Collections.unmodifiableList(columns);
	}
	
	public Props getReceiverProps() {
		return receiverProps;
	}

	@Override
	public String toString() {
		return "GetDataset [id=" + id + ", columns=" + columns
				+ ", receiverProps=" + receiverProps + "]";
	}	
}

package nl.idgis.publisher.harvester.sources.messages;

import java.util.Collections;
import java.util.List;

import akka.actor.Props;

public class FetchVectorDataset extends FetchDataset {		

	private static final long serialVersionUID = 3398760201678202071L;	
	
	private final List<String> columns;	
	
	public FetchVectorDataset(String id, List<String> columns, Props receiverProps) {
		super(id, receiverProps);
		
		this.columns = columns;
	}	
	
	public List<String> getColumns() {
		return Collections.unmodifiableList(columns);
	}

	@Override
	public String toString() {
		return "FetchVectorDataset [id=" + id + ", columns=" + columns
				+ ", receiverProps=" + receiverProps + "]";
	}	
}

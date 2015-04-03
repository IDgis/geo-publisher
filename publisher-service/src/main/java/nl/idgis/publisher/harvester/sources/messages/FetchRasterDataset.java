package nl.idgis.publisher.harvester.sources.messages;

import akka.actor.Props;

public class FetchRasterDataset extends FetchDataset {

	private static final long serialVersionUID = 5866750845747931189L;

	public FetchRasterDataset(String id, Props receiverProps) {
		super(id, receiverProps);
	}

	@Override
	public String toString() {
		return "FetchRasterDataset [id=" + id + ", receiverProps="
				+ receiverProps + "]";
	}
	
}

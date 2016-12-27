package nl.idgis.publisher.provider;

import nl.idgis.publisher.provider.sde.messages.GeodatabaseItem;

import akka.actor.UntypedActor;

public class GeodatabaseItemVectorDatasetInfoBuilder extends UntypedActor {

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof GeodatabaseItem) {
			
		} else {
			unhandled(msg);
		}
	}

}

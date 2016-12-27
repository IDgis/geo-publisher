package nl.idgis.publisher.provider;

import java.io.Serializable;

import nl.idgis.publisher.provider.sde.messages.GeodatabaseItem;
import nl.idgis.publisher.provider.sde.messages.GetGeodatabaseItem;

import akka.actor.Props;

public class GeodatabaseItemDatasetInfoSourceDesc implements DatasetInfoSourceDesc, Serializable {

	private static final long serialVersionUID = 7365740756594258116L;
	
	private final Props props;
	
	GeodatabaseItemDatasetInfoSourceDesc(Props props) {
		this.props = props;
	}

	@Override
	public Props getProps() {
		return props;
	}

	@Override
	public Class<?> getType() {
		return GeodatabaseItem.class;
	}

	@Override
	public Object getRequest(String identification) {
		return new GetGeodatabaseItem(identification);
	}

}

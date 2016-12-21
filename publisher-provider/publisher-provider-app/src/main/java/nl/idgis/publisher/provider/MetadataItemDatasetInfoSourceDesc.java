package nl.idgis.publisher.provider;

import java.io.Serializable;

import nl.idgis.publisher.provider.metadata.messages.GetMetadata;
import nl.idgis.publisher.provider.metadata.messages.MetadataItem;

import akka.actor.Props;

public class MetadataItemDatasetInfoSourceDesc implements DatasetInfoSourceDesc, Serializable {	

	private static final long serialVersionUID = 2153694147992447679L;
	
	private final Props props;
	
	public MetadataItemDatasetInfoSourceDesc(Props props) {
		this.props = props;
	}

	@Override
	public Props getProps() {
		return props;
	}

	@Override
	public Class<?> getType() {
		return MetadataItem.class;
	}

	@Override
	public Object getRequest(String identification) {
		return new GetMetadata(identification);
	}

}

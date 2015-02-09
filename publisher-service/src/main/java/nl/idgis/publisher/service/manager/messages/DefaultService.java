package nl.idgis.publisher.service.manager.messages;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultService extends DefaultGroupLayer implements Service {	
	
	private static final long serialVersionUID = -2338176011245233859L;
	
	public DefaultService(String id, List<Dataset> datasets, Map<String, String> groups) {					
		super(id, datasets.stream()
			.collect(Collectors.toMap(
				dataset -> dataset.getId(), 
				dataset -> new DefaultDatasetLayer(dataset))), groups);
	}
	
	protected boolean filterGroup(Map.Entry<String, String> groupEntry) {
		return groupEntry.getValue() == null;
	}
}

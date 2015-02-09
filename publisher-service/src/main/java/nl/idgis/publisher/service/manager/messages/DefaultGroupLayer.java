package nl.idgis.publisher.service.manager.messages;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultGroupLayer extends AbstractLayer implements GroupLayer {
	
	private static final long serialVersionUID = -5112614407998270385L;

	private final Map<String, DatasetLayer> datasets;
	
	private final Map<String, String> groups;
	
	public DefaultGroupLayer(String id, Map<String, DatasetLayer> datasets, Map<String, String> groups) {
		super(id, true);
		
		this.datasets = datasets;
		this.groups = groups;
	}
	
	private Layer asLayer(String id) {
		if(datasets.containsKey(id)) {
			return datasets.get(id);
		}
		
		return new DefaultGroupLayer(id, datasets, groups);
	}
	
	protected boolean filterGroup(Map.Entry<String, String> groupEntry) {
		return id.equals(groupEntry.getValue());
	}

	@Override
	public List<Layer> getLayers() {
		return groups.entrySet().stream()
			.filter(this::filterGroup)
			.map(groupEntry -> asLayer(groupEntry.getKey()))
			.collect(Collectors.toList());
	}	
}

package nl.idgis.publisher.domain.query;

import java.util.List;

import nl.idgis.publisher.domain.response.Response;

public class PutGroupStructure implements DomainQuery<Response<?>>{

	private static final long serialVersionUID = 1365398099476011167L;
	
	private final String groupId;
	
	private final List<String> layerIds;
	
	private final List<String> layerStyleIds;
	
	public PutGroupStructure (final String groupId, final List<String> layerIds, final List<String> layerStyleIds) {
		this.groupId = groupId;
		this.layerIds = layerIds;
		this.layerStyleIds = layerStyleIds;
	}
	
	public String groupId () {
		return this.groupId;
	}

	public List<String> layerIdList() {
		return layerIds;
	}
	
	public List<String> layerStyleIdList() {
		return layerStyleIds;
	}
	
}
 
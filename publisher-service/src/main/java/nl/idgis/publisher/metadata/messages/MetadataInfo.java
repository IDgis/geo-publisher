package nl.idgis.publisher.metadata.messages;

import java.util.List;

import com.mysema.query.Tuple;

import nl.idgis.publisher.domain.web.tree.Service;

public class MetadataInfo {

	private final List<Tuple> joinTuples;
	
	private final List<Service> serviceInfo;

	public MetadataInfo(List<Tuple> joinTuples, List<Service> serviceInfo) {
		this.joinTuples = joinTuples;
		this.serviceInfo = serviceInfo;
	}

	public List<Tuple> getJoinTuples() {
		return joinTuples;
	}

	public List<Service> getServiceInfo() {
		return serviceInfo;
	}
}

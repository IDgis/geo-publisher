package nl.idgis.publisher.metadata.messages;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import com.mysema.query.Tuple;

import nl.idgis.publisher.domain.web.tree.Service;

public class MetadataInfo implements Serializable {	

	private static final long serialVersionUID = -4466982583083563284L;

	private final List<Tuple> joinTuples;
	
	private final List<Service> serviceInfo;

	public MetadataInfo(List<Tuple> joinTuples, List<Service> serviceInfo) {
		this.joinTuples = Objects.requireNonNull(joinTuples, "joinTuples must not be null");
		this.serviceInfo = Objects.requireNonNull(serviceInfo, "serviceInfo must not be null");
	}

	public List<Tuple> getJoinTuples() {
		return joinTuples;
	}

	public List<Service> getServiceInfo() {
		return serviceInfo;
	}

	@Override
	public String toString() {
		return "MetadataInfo [joinTuples=" + joinTuples + ", serviceInfo=" + serviceInfo + "]";
	}
}

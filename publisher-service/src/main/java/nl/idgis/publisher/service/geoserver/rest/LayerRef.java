package nl.idgis.publisher.service.geoserver.rest;

public class LayerRef {

	private final String layerId;
	
	private final boolean group;
	
	public LayerRef(String layerId, boolean group) {
		this.layerId = layerId;
		this.group = group;
	}
	
	public String getLayerId() {
		return layerId;
	}
	
	public boolean isGroup() {
		return group;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (group ? 1231 : 1237);
		result = prime * result + ((layerId == null) ? 0 : layerId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LayerRef other = (LayerRef) obj;
		if (group != other.group)
			return false;
		if (layerId == null) {
			if (other.layerId != null)
				return false;
		} else if (!layerId.equals(other.layerId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "LayerRef [layerId=" + layerId + ", group=" + group + "]";
	}
}

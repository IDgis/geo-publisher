package nl.idgis.publisher.service.provisioning;

import java.io.Serializable;

public class ServiceInfo implements Serializable {

	private static final long serialVersionUID = -8251074653421202652L;

	private final ConnectionInfo service;
	
	private final String rasterFolder;
	
	public ServiceInfo(ConnectionInfo service, String rasterFolder) {
		this.service = service;		
		this.rasterFolder = rasterFolder;
	}

	public ConnectionInfo getService() {
		return service;
	}

	public String getRasterFolder() {
		return rasterFolder;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((rasterFolder == null) ? 0 : rasterFolder.hashCode());
		result = prime * result + ((service == null) ? 0 : service.hashCode());
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
		ServiceInfo other = (ServiceInfo) obj;
		if (rasterFolder == null) {
			if (other.rasterFolder != null)
				return false;
		} else if (!rasterFolder.equals(other.rasterFolder))
			return false;
		if (service == null) {
			if (other.service != null)
				return false;
		} else if (!service.equals(other.service))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ServiceInfo [service=" + service + ", rasterFolder=" + rasterFolder + "]";
	}

	
}

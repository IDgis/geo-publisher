package nl.idgis.publisher.service.provisioning;

import java.io.Serializable;

public class ServiceInfo implements Serializable {

	private static final long serialVersionUID = -5328308866073768223L;

	private final ConnectionInfo service;
	
	private final ConnectionInfo database;
	
	private final String rasterFolder;
	
	public ServiceInfo(ConnectionInfo service, ConnectionInfo database, String rasterFolder) {
		this.service = service;
		this.database = database;
		this.rasterFolder = rasterFolder;
	}

	public ConnectionInfo getService() {
		return service;
	}

	public ConnectionInfo getDatabase() {
		return database;
	}
	
	public String getRasterFolder() {
		return rasterFolder;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((database == null) ? 0 : database.hashCode());
		result = prime * result
				+ ((rasterFolder == null) ? 0 : rasterFolder.hashCode());
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
		if (database == null) {
			if (other.database != null)
				return false;
		} else if (!database.equals(other.database))
			return false;
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
		return "ServiceInfo [service=" + service + ", database=" + database
				+ ", rasterFolder=" + rasterFolder + "]";
	}	
}

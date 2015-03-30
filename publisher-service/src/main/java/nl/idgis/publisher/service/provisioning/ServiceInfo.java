package nl.idgis.publisher.service.provisioning;

import java.io.Serializable;

public class ServiceInfo implements Serializable {

	private static final long serialVersionUID = 751619442106975726L;

	private final ConnectionInfo service;
	
	private final ConnectionInfo database;
	
	public ServiceInfo(ConnectionInfo service, ConnectionInfo database) {
		this.service = service;
		this.database = database;
	}

	public ConnectionInfo getService() {
		return service;
	}

	public ConnectionInfo getDatabase() {
		return database;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((database == null) ? 0 : database.hashCode());
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
				+ "]";
	}
	
}

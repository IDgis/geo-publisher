package nl.idgis.publisher.provider.database.messages;

import java.io.Serializable;

import nl.idgis.publisher.domain.service.Type;

abstract public class AbstractDatabaseColumnInfo implements Serializable {

	private static final long serialVersionUID = 8052868017910750435L;

	private final String name;
	
	final String typeName;

	private String vendor = "";
	
	public AbstractDatabaseColumnInfo(String name, String typeName) {
		this.name = name;
		this.typeName = typeName;
	}

	public AbstractDatabaseColumnInfo(String name, String typeName, String vendor) {
		this(name, typeName);
		this.vendor =  vendor;
	}

	public String getName() {
		return name;
	}

	public String getTypeName() {
		return typeName;
	}

	public String getVendor() {
		return vendor;
	}

	abstract public Type getType();


	@Override
	public String toString() {
		return "AbstractDatabaseColumnInfo [vendor="+ vendor+ ", name=" + name + ", typeName=" + typeName + "]";
	}
}

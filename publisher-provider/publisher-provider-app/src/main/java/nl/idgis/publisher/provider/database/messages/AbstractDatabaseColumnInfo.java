package nl.idgis.publisher.provider.database.messages;

import java.io.Serializable;

import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.provider.database.DatabaseType;

abstract public class AbstractDatabaseColumnInfo implements Serializable {

	private static final long serialVersionUID = 334332451552136350L;

	private final String name;
	
	final String typeName;

	private DatabaseType vendor;
	
	public AbstractDatabaseColumnInfo(String name, String typeName) {
		this.name = name;
		this.typeName = typeName;
	}

	public AbstractDatabaseColumnInfo(String name, String typeName, DatabaseType vendor) {
		this(name, typeName);
		this.vendor =  vendor;
	}

	public String getName() {
		return name;
	}

	public String getTypeName() {
		return typeName;
	}

	public DatabaseType getVendor() {
		return vendor;
	}

	abstract public Type getType();


	@Override
	public String toString() {
		return "AbstractDatabaseColumnInfo [vendor="+ vendor+ ", name=" + name + ", typeName=" + typeName + "]";
	}
}

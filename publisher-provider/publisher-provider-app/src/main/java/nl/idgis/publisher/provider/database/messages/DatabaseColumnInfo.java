package nl.idgis.publisher.provider.database.messages;

import java.io.Serializable;

import nl.idgis.publisher.domain.service.Type;

public class DatabaseColumnInfo implements Serializable {

	private static final long serialVersionUID = 8052868017910750424L;

	private final String name;
	
	private final String typeName;
	
	public DatabaseColumnInfo(String name, String typeName) {
		this.name = name;
		this.typeName = typeName;
	}

	public String getName() {
		return name;
	}

	public String getTypeName() {
		return typeName;
	}
	
	public Type getType() {
		switch(typeName.toUpperCase()) {
			case "NUMBER":
				return Type.NUMERIC;
			case "DATE":
				return Type.DATE;
			case "VARCHAR2":
			case "NVARCHAR2":
			case "NCHAR":
			case "CHAR":
				return Type.TEXT;
			case "SDO_GEOMETRY":
				return Type.GEOMETRY;
		}
		
		return null;
	}

	@Override
	public String toString() {
		return "DatabaseColumnInfo [name=" + name + ", typeName=" + typeName + "]";
	}
}

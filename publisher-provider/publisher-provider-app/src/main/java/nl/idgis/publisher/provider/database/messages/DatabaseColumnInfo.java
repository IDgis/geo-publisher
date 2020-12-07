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

	public Type getTypePostgres() {
		switch(typeName.toUpperCase()) {
			case "INTEGER":
			case "NUMERIC":
				return Type.NUMERIC;
			//case "DATE":
			//case "TIMESTAMP(6)":
			//	return Type.DATE;
			case "CHARACTER VARYING(32)":
				return Type.TEXT;
			case "GEOMETRY(POLYGON,28992)":
				return Type.GEOMETRY;
		}

		return null;
	}

	public Type getType() {
		switch(typeName.toUpperCase()) {
			case "NUMBER":
			case "FLOAT":
				return Type.NUMERIC;
			case "DATE":
			case "TIMESTAMP(6)":
				return Type.DATE;
			case "VARCHAR2":
			case "NVARCHAR2":
			case "NCHAR":
			case "CHAR":
			case "CLOB":
			case "NCLOB":
				return Type.TEXT;
			case "SDO_GEOMETRY":
			case "ST_GEOMETRY":
				return Type.GEOMETRY;
		}
		
		return null;
	}

	@Override
	public String toString() {
		return "DatabaseColumnInfo [name=" + name + ", typeName=" + typeName + "]";
	}
}

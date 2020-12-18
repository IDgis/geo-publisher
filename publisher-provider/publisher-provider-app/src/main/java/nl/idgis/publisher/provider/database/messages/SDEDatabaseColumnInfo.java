package nl.idgis.publisher.provider.database.messages;

import nl.idgis.publisher.domain.service.Type;

public class SDEDatabaseColumnInfo extends AbstractDatabaseColumnInfo {

	private static final long serialVersionUID = 8052868017910750438L;

	public SDEDatabaseColumnInfo(String name, String typeName) {
		super(name, typeName, "SDE");
	}

	@Override
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
}

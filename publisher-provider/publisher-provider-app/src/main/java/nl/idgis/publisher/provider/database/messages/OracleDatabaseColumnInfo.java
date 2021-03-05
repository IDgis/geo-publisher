package nl.idgis.publisher.provider.database.messages;

import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.provider.database.DatabaseType;

public class OracleDatabaseColumnInfo extends AbstractDatabaseColumnInfo {

	private static final long serialVersionUID = -404115030380853646L;

	public OracleDatabaseColumnInfo(String name, String typeName) {
		super(name, typeName, DatabaseType.ORACLE);
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

package nl.idgis.publisher.provider.database.messages;

import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.provider.database.DatabaseType;

public class PostgresDatabaseColumnInfo extends AbstractDatabaseColumnInfo {

	private static final long serialVersionUID = -2871088454751961295L;

	public PostgresDatabaseColumnInfo(String name, String typeName) {
		super(name, typeName, DatabaseType.POSTGRES);
	}

	@Override
	public Type getType() {
		int separatorIndex = typeName.indexOf("(");
		String typeToTest = separatorIndex == -1 ? typeName : typeName.substring(0, separatorIndex);

		switch(typeToTest.toUpperCase()) {
			case "SMALLINT":
			case "BIGINT":
			case "INTEGER":
			case "NUMERIC":
			case "DECIMAL":
			case "REAL":
			case "DOUBLE":
			case "DOUBLE PRECISION":
				return Type.NUMERIC;
			case "DATE":
			case "TIMESTAMP":
			case "TIMESTAMP WITHOUT TIME ZONE":
				return Type.DATE;
			case "CHARACTER VARYING": // CHARACTER VARYING(32) etc.
			case "CHARACTER": // CHARACTER(21) etc.
			case "TEXT":
			case "CHAR":
			case "CLOB":
			case "XML":
				return Type.TEXT;
			case "GEOMETRY": // Geometry(POLYGON, 28992) etc.
			case "ST_GEOMETRY":
			case "ST_POINT":
			case "ST_LINESTRING":
			case "ST_POLYGON":
			case "ST_MULTIPOINT":
			case "ST_MULTILINESTRING":
			case "ST_MULTIPOLYGON":
				return Type.GEOMETRY;
			case "BOOLEAN":
				return Type.BOOLEAN;
		}

		return null;
	}
}

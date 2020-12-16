package nl.idgis.publisher.provider.database.messages;

import nl.idgis.publisher.domain.service.Type;

public class PostgresDatabaseColumnInfo extends AbstractDatabaseColumnInfo {

	private static final long serialVersionUID = 8052868017910750437L;

	public PostgresDatabaseColumnInfo(String name, String typeName) {
		super(name, typeName, "postgres");
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
				return Type.TEXT;
			case "GEOMETRY": // Geometry(POLYGON, 28992) etc.
				return Type.GEOMETRY;
			case "BOOLEAN":
				return Type.BOOLEAN;
		}

		return null;
	}
}

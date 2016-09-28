package nl.idgis.publisher.database.messages;

public class ReplaceTable extends Query {

	private static final long serialVersionUID = -6178837583711072722L;
	
	private final String schemaName, fromTable, toTable;

	public ReplaceTable(String schemaName, String fromTable, String toTable) {
		this.schemaName = schemaName;
		this.fromTable = fromTable;
		this.toTable = toTable;
	}

	public String getSchemaName() {
		return schemaName;
	}

	public String getFromTable() {
		return fromTable;
	}

	public String getToTable() {
		return toTable;
	}

	@Override
	public String toString() {
		return "ReplaceTable [schemaName=" + schemaName + ", fromTable=" + fromTable + ", toTable=" + toTable + "]";
	}
}

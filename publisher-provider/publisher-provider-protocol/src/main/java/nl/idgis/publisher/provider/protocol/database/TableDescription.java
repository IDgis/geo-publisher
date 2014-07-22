package nl.idgis.publisher.provider.protocol.database;

import java.io.Serializable;
import java.util.Arrays;

public class TableDescription implements Serializable {
	
	private static final long serialVersionUID = 4857122532444764437L;
	
	private final Column[] columns;
	
	public TableDescription(Column[] columns) {
		this.columns = columns;
	}

	public Column[] getColumns() {
		return Arrays.copyOf(columns, columns.length);
	}

	@Override
	public String toString() {
		return "TableDescription [columns=" + Arrays.toString(columns) + "]";
	}
}

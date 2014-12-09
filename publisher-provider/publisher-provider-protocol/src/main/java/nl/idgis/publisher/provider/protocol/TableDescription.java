package nl.idgis.publisher.provider.protocol;

import java.io.Serializable;
import java.util.Arrays;

/**
 * The table description of a {@link VectorDatasetInfo}.
 * 
 * @author copierrj
 *
 */
public class TableDescription implements Serializable {
	
	private static final long serialVersionUID = 4857122532444764437L;
	
	private final Column[] columns;
	
	/**
	 * 
	 * @param columns all columns.
	 */
	public TableDescription(Column[] columns) {
		this.columns = columns;
	}

	/**
	 * 
	 * @return columns
	 */
	public Column[] getColumns() {
		return Arrays.copyOf(columns, columns.length);
	}

	@Override
	public String toString() {
		return "TableDescription [columns=" + Arrays.toString(columns) + "]";
	}
}

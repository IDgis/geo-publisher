package nl.idgis.publisher.provider.protocol;

import java.io.Serializable;
import java.util.Arrays;

/**
 * The table description of a {@link VectorDatasetInfo}.
 * 
 * @author copierrj
 *
 */
public class TableInfo implements Serializable {
	
	private static final long serialVersionUID = -8130055458220826262L;
	
	private final ColumnInfo[] columns;
	
	/**
	 * 
	 * @param columns all columns.
	 */
	public TableInfo(ColumnInfo[] columns) {
		this.columns = columns;
	}

	/**
	 * 
	 * @return columns
	 */
	public ColumnInfo[] getColumns() {
		return Arrays.copyOf(columns, columns.length);
	}

	@Override
	public String toString() {
		return "TableInfo [columns=" + Arrays.toString(columns) + "]";
	}
}

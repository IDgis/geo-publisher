package nl.idgis.publisher.provider.database.messages;

import java.io.Serializable;
import java.util.Arrays;

public class DatabaseTableInfo implements Serializable {

	private static final long serialVersionUID = -4627181111870670640L;
	
	private final AbstractDatabaseColumnInfo[] columns;
	
	public DatabaseTableInfo(AbstractDatabaseColumnInfo[] columns) {
		this.columns = columns;
	}

	public AbstractDatabaseColumnInfo[] getColumns() {
		return columns;
	}

	@Override
	public String toString() {
		return "DatabaseTableInfo [columns=" + Arrays.toString(columns) + "]";
	}
}

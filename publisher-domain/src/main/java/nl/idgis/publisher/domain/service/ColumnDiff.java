package nl.idgis.publisher.domain.service;

import java.io.Serializable;

public final class ColumnDiff implements Serializable {
	private static final long serialVersionUID = -8575827236061364000L;
	
	private final Column column;
	private final ColumnDiffOperation operation;
	
	public ColumnDiff (final Column column, final ColumnDiffOperation operation) {
		if (column == null) {
			throw new NullPointerException ("column cannot be null");
		}
		if (operation == null) {
			throw new NullPointerException ("operation cannot be null");
		}
		
		this.column = column;
		this.operation = operation;
	}

	public Column getColumn () {
		return column;
	}

	public ColumnDiffOperation getOperation () {
		return operation;
	}
}

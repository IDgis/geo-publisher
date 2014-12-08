package nl.idgis.publisher.provider.mock.messages;

import java.io.Serializable;

import nl.idgis.publisher.provider.protocol.TableDescription;

public class PutTableInfo implements Serializable {

	private static final long serialVersionUID = -4287280295749364302L;
	
	private final String tableName;
	private final TableDescription tableDescription;
	private final long numberOfRecords;
	
	public PutTableInfo(String tableName, TableDescription tableDescription, long numberOfRecords) {
		this.tableName = tableName;
		this.tableDescription = tableDescription;
		this.numberOfRecords = numberOfRecords;
	}

	public String getTableName() {
		return tableName;
	}

	public TableDescription getTableDescription() {
		return tableDescription;
	}

	public long getNumberOfRecords() {
		return numberOfRecords;
	}

	@Override
	public String toString() {
		return "PutTableInfo [tableName=" + tableName
				+ ", tableDescription=" + tableDescription
				+ ", numberOfRecords=" + numberOfRecords + "]";
	}
}

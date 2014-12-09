package nl.idgis.publisher.provider.protocol;

import java.util.List;

public class GetVectorDataset extends GetDataset {
	
	private static final long serialVersionUID = 7005459085925901438L;

	private final List<String> columnNames;
	
	private final int messageSize;

	public GetVectorDataset(String identification, List<String> columnNames, int messageSize) {
		super(identification);
		
		this.columnNames = columnNames;
		this.messageSize = messageSize;
	}
	
	public List<String> getColumnNames() {
		return columnNames;
	}
	
	public int getMessageSize() {
		return messageSize;
	}

	@Override
	public String toString() {
		return "GetVectorDataset [columnNames=" + columnNames
				+ ", messageSize=" + messageSize + "]";
	}

}

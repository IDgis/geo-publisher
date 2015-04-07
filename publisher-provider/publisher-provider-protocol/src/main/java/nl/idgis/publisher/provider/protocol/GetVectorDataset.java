package nl.idgis.publisher.provider.protocol;

import java.util.List;

/**
 * Request a vector dataset.
 *  
 * @author copierrj
 *
 */
public class GetVectorDataset extends AbstractGetDatasetRequest {
	
	private static final long serialVersionUID = 7005459085925901438L;

	private final List<String> columnNames;
	
	private final int messageSize;

	/**
	 * Creates a get vector dataset request.
	 * @param identification the identifier of the dataset.
	 * @param columnNames a list of column names to request.
	 * @param messageSize specifies how many {@link Record} objects are packed into a single {@link Records} object.
	 */
	public GetVectorDataset(String identification, List<String> columnNames, int messageSize) {
		super(identification);
		
		this.columnNames = columnNames;
		this.messageSize = messageSize;
	}
	
	/**
	 * 
	 * @return list of column names
	 */
	public List<String> getColumnNames() {
		return columnNames;
	}
	
	/**
	 * 
	 * @return how many {@link Record} objects are to be packed into a single {@link Records} object?
	 */
	public int getMessageSize() {
		return messageSize;
	}

	@Override
	public String toString() {
		return "GetVectorDataset [columnNames=" + columnNames
				+ ", messageSize=" + messageSize + ", identification="
				+ getIdentification() + "]";
	}	

}

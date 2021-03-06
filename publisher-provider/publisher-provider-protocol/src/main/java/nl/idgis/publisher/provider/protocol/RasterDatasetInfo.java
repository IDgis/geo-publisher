package nl.idgis.publisher.provider.protocol;

import java.time.ZonedDateTime;
import java.util.Set;

import nl.idgis.publisher.domain.Log;

/**
 * A description of a raster dataset.
 * 
 * @author copierrj
 *
 */
public class RasterDatasetInfo extends DatasetInfo {
	
	private static final long serialVersionUID = -8448537523977967833L;
	
	private final RasterFormat format;
	
	private final long size;

	/**
	 * 
	 * @param identification the identifier of the dataset.
	 * @param title the title of the dataset.
	 * @param alternateTitle the alternate title of the dataset.
	 * @param categoryId the identifier of the category for this dataset
	 * @param revisionDate the revision date of this dataset
	 * @param attachments the attachments of the datasets.
	 * @param logs logs for the dataset.
	 * @param format file format of the dataset.
	 * @param size size of the dataset
	 */
	public RasterDatasetInfo(String identification, String title, String alternateTitle, String categoryId, ZonedDateTime revisionDate, Set<Attachment> attachments, 
		Set<Log> logs, RasterFormat format, long size) {
		
		super(identification, title, alternateTitle, categoryId, revisionDate, attachments, logs);
		
		this.format = format;
		this.size = size;
	}

	/**
	 * 
	 * @return the file format of the dataset
	 */
	public RasterFormat getFormat() {
		return format;
	}
	
	/**
	 * 
	 * @return the size of the dataset
	 */
	public long getSize() {
		return size;
	}

	@Override
	public String toString() {
		return "RasterDatasetInfo [format=" + format + ", size=" + size
				+ ", identification=" + identification + ", title=" + title
				+ ", alternateTitle=" + alternateTitle + ", categoryId="
				+ categoryId + ", revisionDate=" + revisionDate
				+ ", attachments=" + attachments + ", logs=" + logs + "]";
	}
	
}

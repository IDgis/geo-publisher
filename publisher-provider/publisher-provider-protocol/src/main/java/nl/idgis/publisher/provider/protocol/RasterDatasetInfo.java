package nl.idgis.publisher.provider.protocol;

import java.util.Date;
import java.util.Set;

import nl.idgis.publisher.domain.Log;

/**
 * A description of a raster dataset.
 * 
 * @author copierrj
 *
 */
public class RasterDatasetInfo extends DatasetInfo {	

	private static final long serialVersionUID = -352747575039672262L;
	
	private final RasterFormat format;

	/**
	 * 
	 * @param identification the identifier of the dataset.
	 * @param title the title of the dataset.
	 * @param alternateTitle the alternate title of the dataset.
	 * @param categoryId the identifier of the category for this dataset
	 * @param revisionDate the revision date of this dataset
	 * @param attachments the attachments of the datasets.
	 * @param logs logs for the dataset.
	 * @param format file format for the dataset.
	 */
	public RasterDatasetInfo(String identification, String title, String alternateTitle, String categoryId, Date revisionDate, Set<Attachment> attachments, 
		Set<Log> logs, RasterFormat format) {
		
		super(identification, title, alternateTitle, categoryId, revisionDate, attachments, logs);
		
		this.format = format;
	}

	/**
	 * 
	 * @return the file format of the dataset
	 */
	public RasterFormat getFormat() {
		return format;
	}

	@Override
	public String toString() {
		return "RasterDatasetInfo [format=" + format + "]";
	}

}

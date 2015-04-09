package nl.idgis.publisher.admin.messages;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;

import nl.idgis.publisher.domain.SourceDatasetType;
import nl.idgis.publisher.domain.service.DatasetLog;
import nl.idgis.publisher.domain.service.DatasetLogType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysema.query.annotations.QueryProjection;

public class SourceDatasetInfo implements Serializable {

	private static final long serialVersionUID = 1483600283295264723L;
	
	private String dataSourceId, dataSourceName;
	private final String id, name, alternateTitle;
	private final String categoryId, categoryName;
	private Long count;
	private SourceDatasetType type;
	
	private DatasetLogType lastLogType; 
	private DatasetLog<?> lastLogParameters;
	private Timestamp lastLogTime;
	
	private Timestamp deleteTime;
	
	private final boolean confidential;

	@QueryProjection
	public SourceDatasetInfo(String id, String name, String alternateTitle, String dataSourceId,
			String dataSourceName, String categoryId, String categoryName,
			Long count, final String type,
			final String lastLogType,
			final String lastLogParameters,
			final Timestamp lastLogTime,
			final Timestamp deleteTime,
			final boolean confidential) {
		super();
		
		if (type == null) {
			throw new NullPointerException ("type cannot be null");
		}
		
		this.id = id;
		this.name = name;
		this.alternateTitle = alternateTitle;
		this.dataSourceId = dataSourceId;
		this.dataSourceName = dataSourceName;
		this.categoryId = categoryId;
		this.categoryName = categoryName;
		this.count = count;
		this.type = SourceDatasetType.valueOf (type);
		this.lastLogType = lastLogType == null ? null : DatasetLogType.valueOf (lastLogType);
		this.lastLogParameters = lastLogParameters == null || lastLogParameters.isEmpty () ? null : parseLogParameters (this.lastLogType, lastLogParameters); 
		this.lastLogTime = lastLogTime;
		this.deleteTime = deleteTime;
		this.confidential = confidential;
	}

	private DatasetLog<?> parseLogParameters (final DatasetLogType logType, final String value) {
		if (logType.getContentClass () == null) {
			return null;
		}
		
		try {
			return new ObjectMapper ()
				.readValue (value, logType.getContentClass ());
		} catch (IOException e) {
			throw new IllegalStateException (e);
		}
	}
	
	public String getDataSourceId() {
		return dataSourceId;
	}

	public String getDataSourceName() {
		return dataSourceName;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	public String getAlternateTitle() {
		return alternateTitle;
	}

	public String getCategoryId() {
		return categoryId;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public Long getCount() {
		return count;
	}

	public SourceDatasetType getType () {
		return type;
	}

	public DatasetLogType getLastLogType () {
		return lastLogType;
	}

	public DatasetLog<?> getLastLogParameters () {
		return lastLogParameters;
	}

	public Timestamp getLastLogTime () {
		return lastLogTime;
	}
	
	public Timestamp getDeleteTime () {
		return deleteTime;
	}
	
	public boolean isConfidential () {
		return confidential;
	}
}

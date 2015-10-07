package nl.idgis.publisher.metadata.messages;

import java.util.Objects;
import java.util.Set;

import nl.idgis.publisher.metadata.ServiceMetadataGenerator;

/**
 * Contains all dataset information required by {@link ServiceMetadataGenerator}.
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
public class ServiceInfo extends MetadataItemInfo {
	
	private static final long serialVersionUID = 7108955413887570141L;

	private final String name, title, alternateTitle, abstr;
	
	private final Set<DatasetRef> datasetRefs;
	
	private final String wmsMetadataId;
	
	private final String wfsMetadataId;
	
	private final Set<String> keywords;
	
	public ServiceInfo(String serviceId, String name, String title, String alternateTitle, String abstr,
		String wmsMetadataId, String wfsMetadataId, Set<DatasetRef> datasetRefs, Set<String> keywords) {
		
		super(serviceId);
		
		this.name = Objects.requireNonNull(name, "name must not be null");
		this.title = title;
		this.alternateTitle = alternateTitle;
		this.abstr = abstr;
		this.wmsMetadataId = Objects.requireNonNull(wmsMetadataId, "wmsMetadataId must not be null");
		this.wfsMetadataId = Objects.requireNonNull(wfsMetadataId, "wfsMetadataId must not be null");
		this.datasetRefs = Objects.requireNonNull(datasetRefs, "datasetRefs must not be null");
		this.keywords = Objects.requireNonNull(keywords, "keywords must not be null");
	}
	
	public String getName() {
		return name;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getAlternateTitle() {
		return alternateTitle;
	}
	
	public String getAbstract() {
		return abstr;
	}

	public String getWMSMetadataId() {
		return wmsMetadataId;
	}

	public String getWFSMetadataId() {
		return wfsMetadataId;
	}

	public Set<DatasetRef> getDatasetRefs() {
		return datasetRefs;
	}
	
	public Set<String> getKeywords() {
		return keywords;
	}

	@Override
	public String toString() {
		return "ServiceInfo [name=" + name + ", title=" + title + ", alternateTitle=" + alternateTitle + ", abstr="
				+ abstr + ", datasetRefs=" + datasetRefs + ", wmsMetadataId=" + wmsMetadataId + ", wfsMetadataId="
				+ wfsMetadataId + ", keywords=" + keywords + "]";
	}	
	
}
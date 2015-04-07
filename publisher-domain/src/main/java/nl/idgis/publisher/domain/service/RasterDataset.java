package nl.idgis.publisher.domain.service;

import java.util.Date;
import java.util.Set;

import nl.idgis.publisher.domain.Log;

public class RasterDataset extends Dataset {
	
	private static final long serialVersionUID = -4309338390795613262L;

	public RasterDataset(String id, String name, String alternateTitle, String categoryId, Date revisionDate, Set<Log> logs) {
		super(id, name, alternateTitle, categoryId, revisionDate, logs);		
	}	
}

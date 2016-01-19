package controller;

import javax.inject.Inject;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;

import play.mvc.Controller;
import play.mvc.Result;
import util.QueryDSL;

import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QPublishedServiceDataset.publishedServiceDataset;

public class Dataset extends Controller {
	
	private final QueryDSL q;
	
	@Inject
	public Dataset(QueryDSL q) {
		this.q = q;
	}

	public Result resource(String name) throws Exception {
		if(name.toLowerCase().endsWith(".xml")) {
			String id = name.substring(0, name.length() - 4);
			
			return q.withTransaction(tx -> {
				Tuple t = tx.query().from(dataset)
					.join(sourceDataset).on(sourceDataset.id.eq(dataset.sourceDatasetId))
					.join(sourceDatasetVersion).on(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
					.where(sourceDatasetVersion.id.in(new SQLSubQuery().from(sourceDatasetVersion)
						.where(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
						.list(sourceDatasetVersion.id.max())))
					.where(sourceDatasetVersion.confidential.isFalse())
					.where(new SQLSubQuery().from(publishedServiceDataset)
						.where(publishedServiceDataset.datasetId.eq(dataset.id))
						.exists())
					.where(dataset.metadataFileIdentification.eq(id))
					.singleResult(sourceDataset.identification, dataset.identification);
				
				if(t == null) {
					return notFound("Not found, id:"+ id);
				}
				
				return ok("Found: " + t.get(sourceDataset.identification) + ", " + t.get(dataset.identification));
			});
		}
		
		return notFound("Not found");
	}
}

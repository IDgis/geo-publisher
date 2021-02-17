package controllers;

import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QPublishedServiceDataset.publishedServiceDataset;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetMetadata.sourceDatasetMetadata;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.expr.BooleanExpression;

import util.QueryDSL.Transaction;
import util.Security;

@Singleton
class DatasetQueryBuilder {
	
	private final Security s;
	
	@Inject
	public DatasetQueryBuilder(Security s) {
		this.s = s;
	}
	
	private SQLQuery joinSourceDatasetVersion(SQLQuery query) {
		query
			.join(sourceDatasetVersion).on(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
			.where(sourceDatasetVersion.id.in(new SQLSubQuery().from(sourceDatasetVersion)
				.where(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
				.list(sourceDatasetVersion.id.max())));
		
		if(s.isTrusted()) {
			return query;
		} else {
			return query.where(sourceDatasetVersion.metadataConfidential.isFalse());
		}
	}
	
	BooleanExpression isPublishedDataset() {
		return new SQLSubQuery().from(publishedServiceDataset)
			.where(publishedServiceDataset.datasetId.eq(dataset.id))
			.exists();
	}
	
	SQLQuery fromDataset(Transaction tx) {
		return joinSourceDatasetVersion(tx.query().from(dataset)
			.join(sourceDataset).on(sourceDataset.id.eq(dataset.sourceDatasetId))
			.join(sourceDatasetMetadata).on(sourceDatasetMetadata.sourceDatasetId.eq(sourceDataset.id)));
	}
	
	SQLQuery fromPublishedDataset(Transaction tx) {
		return joinSourceDatasetVersion(tx.query().from(dataset)
			.join(sourceDataset).on(sourceDataset.id.eq(dataset.sourceDatasetId))
			.join(sourceDatasetMetadata).on(sourceDatasetMetadata.sourceDatasetId.eq(sourceDataset.id))
			.where(isPublishedDataset()
				.and(sourceDataset.deleteTime.isNull())
				.and(dataset.filterConditions.eq("{}"))));
	}
	
	SQLQuery fromSourceDataset(Transaction tx) {
		return joinSourceDatasetVersion(
			tx.query().from(sourceDataset)
				.join(sourceDatasetMetadata).on(sourceDatasetMetadata.sourceDatasetId.eq(sourceDataset.id)));
	}
	
	SQLQuery fromNonPublishedSourceDataset(Transaction tx) {
		return joinSourceDatasetVersion(
			tx.query().from(sourceDataset)
				.join(sourceDatasetMetadata).on(sourceDatasetMetadata.sourceDatasetId.eq(sourceDataset.id))
				.where(new SQLSubQuery().from(dataset)
						.where(dataset.sourceDatasetId.eq(sourceDataset.id))
						.where(isPublishedDataset())
						.notExists()
					.and(sourceDataset.deleteTime.isNull())));
	}
	
}

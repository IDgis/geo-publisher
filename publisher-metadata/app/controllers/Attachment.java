package controllers;

import play.mvc.Result;
import util.QueryDSL;

import javax.inject.Inject;

import com.mysema.query.Tuple;

import play.mvc.Controller;

import static nl.idgis.publisher.database.QSourceDatasetMetadataAttachment.sourceDatasetMetadataAttachment;

public class Attachment extends Controller {
	
	private final QueryDSL q;
	
	@Inject
	public Attachment(QueryDSL q) {
		this.q = q;
	}

	public Result get(String id, String file) {
		return q.withTransaction(tx -> {
			Tuple attachment = 
				tx.query().from(sourceDatasetMetadataAttachment)
					.where(sourceDatasetMetadataAttachment.id.eq(Integer.parseInt(id)))
					.singleResult(
						sourceDatasetMetadataAttachment.contentType,
						sourceDatasetMetadataAttachment.contentDisposition,
						sourceDatasetMetadataAttachment.content);
			
			if(attachment == null) {
				return notFound("404 Not Found: " + controllers.routes.Attachment.get(id, file));
			} else {
				String contentType = attachment.get(sourceDatasetMetadataAttachment.contentType);
				if(contentType != null) {
					response().setContentType(contentType);
				}
				
				String contentDisposition = attachment.get(sourceDatasetMetadataAttachment.contentDisposition);
				if(contentDisposition != null) {
					response().setHeader("Content-Disposition", contentDisposition);
				}
				
				return ok(attachment.get(sourceDatasetMetadataAttachment.content));
			}
		});
	}
}

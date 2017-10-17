package controllers;

import java.util.Map.Entry;
import java.util.Optional;

import javax.inject.Inject;

import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.xml.exceptions.NotFound;
import nl.idgis.publisher.xml.exceptions.NotParseable;
import play.libs.F.Promise;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.mvc.Controller;
import play.mvc.Result;
import util.MetadataConfig;

public class DatasetUpdateStylesheet extends Controller {
	
	private final MetadataConfig config;
	
	private final MetadataDocumentFactory mdf;
	
	private final WSClient ws;
	
	@Inject
	public DatasetUpdateStylesheet(MetadataConfig config, MetadataDocumentFactory mdf, WSClient ws) {
		this.config = config;
		this.mdf = mdf;
		this.ws = ws;
	}
	
	public Promise<Result> update(String url) {
		
		WSRequest request = ws.url(url).setFollowRedirects(true).setRequestTimeout(10000);
		
		for(Entry<String, String[]> e : request().queryString().entrySet()) {
			
			int i = 0;
			for(String s : e.getValue()) {
				
				if("url".equals(e.getKey()) && i == 0) {
					i++;
					continue;
				}
				
				request = request.setQueryParameter(e.getKey(), s);
				i++;
			}
		}
		
		return request.get().map(response -> {
			
			try {
				MetadataDocument md = mdf.parseDocument(response.getBodyAsStream());
				
				if(!"ISO 19115".equals(md.getMetadataStandardName())) {
					return internalServerError("This is not an ISO 19115 document");
				}
				
				response().setContentType("application/xml");
				md.removeStylesheet();
				
				Optional<String> stylesheetUrl = config.getMetadataStylesheetPrefix().map(prefix -> {
					return prefix + "datasets/extern/metadata.xsl";
				});
				
				stylesheetUrl.ifPresent(s -> {
					md.setStylesheet(s);
				});
				
				return ok(md.getContent()).as("UTF-8");
			} catch(NotParseable np) {
				return internalServerError("This is not an XML document");
			} catch(IllegalArgumentException iae) {
				return internalServerError("XML document doesn't start with /gmd:MD_Metadata");
			} catch(NotFound nf) {
				return internalServerError("This is not an ISO 19115 document");
			}
		});
	}
}

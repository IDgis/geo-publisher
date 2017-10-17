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
	
	private String[] acceptedDomains;
	
	@Inject
	public DatasetUpdateStylesheet(MetadataConfig config, MetadataDocumentFactory mdf, WSClient ws) {
		this.config = config;
		this.mdf = mdf;
		this.ws = ws;
		
		config.getAcceptedDomainsUpdateStylesheet().ifPresent(domains -> {
			this.acceptedDomains = domains.split(",");
		});
	}
	
	public Promise<Result> update(String url) {
		
		String protocol = url.substring(0, url.indexOf("://") + 3);
		boolean allowedUrl = false;
		
		for(String domain : acceptedDomains) {
			
			if(domain.trim().isEmpty()) {
				// do nothing
			} else {
				String urlCondition = protocol + domain;
				
				if(url.startsWith(urlCondition)) {
					allowedUrl = true;
					break;
				}
			}
		}
		
		WSRequest request = ws.url(url).setFollowRedirects(true).setRequestTimeout(10000);
		
		for(Entry<String, String[]> entry : request().queryString().entrySet()) {
			
			int i = 0;
			for(String value : entry.getValue()) {
				
				if("url".equals(entry.getKey()) && i == 0) {
					i++;
					continue;
				} else {
					request = request.setQueryParameter(entry.getKey(), value);
					i++;
				}
			}
		}
		
		if(allowedUrl) {
			return request.get().map(response -> {
				
				try {
					MetadataDocument md = mdf.parseDocument(response.getBodyAsStream());
					
					if(!"ISO 19115".equals(md.getMetadataStandardName())) {
						return internalServerError("500 Internal Server Error: response is not an ISO 19115 document");
					}
					
					response().setContentType("application/xml");
					md.removeStylesheet();
					
					Optional<String> stylesheetUrl = config.getMetadataStylesheetPrefix().map(prefix -> {
						return prefix + "datasets/extern/metadata.xsl";
					});
					
					stylesheetUrl.ifPresent(stylesheet -> {
						md.setStylesheet(stylesheet);
					});
					
					return ok(md.getContent()).as("UTF-8");
				} catch(NotParseable np) {
					return internalServerError("500 Internal Server Error: response is not an XML document");
				} catch(IllegalArgumentException iae) {
					return internalServerError("500 Internal Server Error: response doesn't start with /gmd:MD_Metadata");
				} catch(NotFound nf) {
					return internalServerError("500 Internal Server Error: response is not an ISO 19115 document");
				}
			});
		} else {
			return Promise.pure(unauthorized("404 Not Found: url doesn't belong to accepted domains"));
		}
	}
}

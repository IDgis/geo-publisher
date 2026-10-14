package controllers;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.xml.transform.TransformerException;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;
import util.MetadataConfig;
import util.QueryDSL;
import util.Security;
import util.Transform;

import nl.idgis.dav.model.Resource;

public class MetadataHTML extends Controller {
	
	private final MetadataConfig config;
	
	private final Security s;
	
	private final QueryDSL q;
	
	private final DatasetQueryBuilder dqb;
	
	@Inject
	public MetadataHTML(MetadataConfig config, Security s, QueryDSL q, DatasetQueryBuilder dqb) throws Exception {
		this.config = config;
		this.s = s;
		this.q = q;
		this.dqb = dqb;
	}
	
	public Result view(String type, String name, Boolean noStyle) throws Exception {
		switch (type) {
			case "dataset":
			case "service":
				break;
			default:
				return internalServerError("Unrecognized type");
		}
		
		Optional<Resource> resource = "dataset".equals(type)
				? new DatasetMetadata(config, q, dqb, s).resource(name)
				: new ServiceMetadata(config, q, s).resource(name);
		
		String base = routes.MetadataHTML.view(type, name, false).absoluteURL(request());
		String access = s.isTrusted() ? "intern" : "extern";
		String stylesheetUrl = config.getMetadataStylesheetPrefix() + type + "s/" + access + "/metadata.xsl";
		
		return resource
				.map(res -> render(res, base, stylesheetUrl, type, name, noStyle))
				.orElse(notFound("404 - not found " + name));
	}
	
	private Result render(Resource res, String base, String stylesheetUrl, String type, String name, Boolean noStyle) {
		try {
			if(noStyle) {
				return ok(res.content()).as("text/xml; charset=utf-8");
			}

			String html = Transform.transformToHtml(base, stylesheetUrl, res.content());
			return ok(html).as("text/html; charset=utf-8");
		} catch (IOException | TransformerException e) {
			Logger.error("XSLT transformation failed for type " + type + " and name " + name, e);
			return internalServerError("Could not display the metadata");
		}
	}
}

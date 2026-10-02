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
	
	public Result view(String type, String name) throws Exception {
		switch (type) {
			case "dataset":
			case "service":
				break;
			default:
				return internalServerError("Unrecognized type");
		}
		
		String base = routes.MetadataHTML.view(type, name).absoluteURL(request());
		
		String access = s.isTrusted() ? "intern" : "extern";
		String stylesheetUrl = config.getMetadataStylesheetPrefix() + type + "s/" + access + "/metadata.xsl";
		
		Optional<Resource> resource = "dataset".equals(type)
				? new DatasetMetadata(config, q, dqb, s).resource(name)
				: new ServiceMetadata(config, q, s).resource(name);

		return resource
				.map(res -> renderHtml(res, base, stylesheetUrl, type, name))
				.orElse(notFound("404 - not found " + name));
	}
	
	private Result renderHtml(Resource res, String base, String stylesheetUrl, String type, String name) {
		try {
			String html = Transform.transformToHtml(base, stylesheetUrl, res.content());
			return ok(html).as("text/html; charset=utf-8");
		} catch (IOException | TransformerException e) {
			Logger.error("XSLT transformation failed for type " + type + " and name " + name, e);
			return internalServerError("Could not display the metadata");
		}
	}
}

package controllers;

import nl.idgis.publisher.domain.web.Service;
import play.Logger;
import play.data.validation.Constraints;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.services.form;
import views.html.services.list;
import actions.DefaultAuthenticator;

@Security.Authenticated (DefaultAuthenticator.class)
public class Services extends Controller {

	public static Promise<Result> createForm () {
		Logger.debug ("create Service");
		
		return Promise.pure (ok (form.render ()));
	}
	
	
	public static Promise<Result> list () {
		Logger.debug ("list Services ");
		
		return Promise.pure (ok (list.render ()));
	}
	
	
	
	public static class ServiceForm {

		
		@Constraints.Required
		private String id;
		@Constraints.Required
		@Constraints.MinLength (1)
		private String name;
		private String title;
		private String alternateTitle;
		private String abstractText;
		private String keywords;
		private String metadata;
		private String watermark;
		
		public ServiceForm (){
			super();
		}
		
		public ServiceForm (final Service service){
			this.id = service.id();
			this.name = service.name();
			this.title = service.title();
			this.alternateTitle = service.alternateTitle();
			this.abstractText = service.abstractText();
			this.keywords = service.keywords();
			this.metadata = service.metadata();
			this.watermark = service.watermark();
			
		}


		public String getId() {
			return id;
		}


		public void setId(String id) {
			this.id = id;
		}


		public String getName() {
			return name;
		}


		public void setName(String name) {
			this.name = name;
		}


		public String getTitle() {
			return title;
		}


		public void setTitle(String title) {
			this.title = title;
		}


		public String getAlternateTitle() {
			return alternateTitle;
		}


		public void setAlternateTitle(String alternateTitle) {
			this.alternateTitle = alternateTitle;
		}


		public String getAbstractText() {
			return abstractText;
		}


		public void setAbstractText(String abstractText) {
			this.abstractText = abstractText;
		}


		public String getKeywords() {
			return keywords;
		}


		public void setKeywords(String keywords) {
			this.keywords = keywords;
		}


		public String getMetadata() {
			return metadata;
		}


		public void setMetadata(String metadata) {
			this.metadata = metadata;
		}


		public String getWatermark() {
			return watermark;
		}


		public void setWatermark(String watermark) {
			this.watermark = watermark;
		}
		
	}
}
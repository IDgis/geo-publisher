package controllers;

import static models.Domain.from;
import models.Domain;
import models.Domain.Function;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.web.Service;
import play.Logger;
import play.Play;
import play.data.Form;
import play.data.validation.Constraints;
import play.libs.Akka;
import play.libs.F;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import actions.DefaultAuthenticator;
import akka.actor.ActorSelection;
import views.html.services.form;
import views.html.services.list;


@Security.Authenticated (DefaultAuthenticator.class)
public class Services extends Controller {

	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");

	private static Promise<Result> renderCreateForm (final Form<ServiceForm> serviceForm) {
		 return Promise.promise(new F.Function0<Result>() {
             @Override
             public Result apply() throws Throwable {
                 return ok (form.render (serviceForm, true));
             }
         });
	}
	
	public static Promise<Result> submitCreateUpdate () {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		final Form<ServiceForm> form = Form.form (ServiceForm.class).bindFromRequest ();
		Logger.debug ("submit Service: " + form.field("name").value());
		
		// validation
		if (form.field("name").value().length() == 1 ) 
			form.reject("name", Domain.message("web.application.page.services.form.field.name.validation.error", "1"));
		if (form.hasErrors ()) {
			return renderCreateForm (form);
		}
		
		final ServiceForm serviceForm = form.get ();
		final Service service = new Service(serviceForm.id, serviceForm.name, serviceForm.title, 
				serviceForm.alternateTitle,serviceForm.abstractText,serviceForm.keywords,serviceForm.metadata,serviceForm.watermark);
		
		return from (database)
			.put(service)
			.executeFlat (new Function<Response<?>, Promise<Result>> () {
				@Override
				public Promise<Result> apply (final Response<?> response) throws Throwable {
					if (CrudOperation.CREATE.equals (response.getOperation())) {
						Logger.debug ("Created service " + service);
						flash ("success", Domain.message("web.application.page.services.name") + " " + serviceForm.getName () + " is " + Domain.message("web.application.added").toLowerCase());
					}else{
						Logger.debug ("Updated service " + service);
						flash ("success", Domain.message("web.application.page.services.name") + " " + serviceForm.getName () + " is " + Domain.message("web.application.updated").toLowerCase());
					}
					return Promise.pure (redirect (routes.Services.list ()));
				}
			});
	}
	
	public static Promise<Result> list () {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);

		Logger.debug ("list Services ");
		
		return from (database)
			.list (Service.class)
			.execute (new Function<Page<Service>, Result> () {
				@Override
				public Result apply (final Page<Service> services) throws Throwable {
					return ok (list.render (services));
				}
			});
	}

	public static Promise<Result> create () {
		Logger.debug ("create Service");
		final Form<ServiceForm> serviceForm = Form.form (ServiceForm.class).fill (new ServiceForm ());
		
		return renderCreateForm (serviceForm);
	}
	
	public static Promise<Result> edit (final String serviceId) {
		Logger.debug ("edit Service: " + serviceId);
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from (database)
			.get (Service.class, serviceId)
			.execute (new Function<Service, Result> () {

				@Override
				public Result apply (final Service service) throws Throwable {
					final Form<ServiceForm> serviceForm = Form
							.form (ServiceForm.class)
							.fill (new ServiceForm (service));
					
					Logger.debug ("Edit serviceForm: " + serviceForm);						

					return ok (form.render (serviceForm, false));
				}
			});
	}

	public static Promise<Result> delete(final String serviceId){
		Logger.debug ("delete Service " + serviceId);
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		from(database).delete(Service.class, serviceId)
		.execute(new Function<Response<?>, Result>() {
			
			@Override
			public Result apply(Response<?> a) throws Throwable {
				System.out.println("apply delete: " + a);
				return null;
			}
		});
		
		return list();
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
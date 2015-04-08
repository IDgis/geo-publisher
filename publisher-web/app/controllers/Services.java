package controllers;

import static models.Domain.from;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import models.Domain;
import models.Domain.Function;
import models.Domain.Function2;
import models.Domain.Function3;
import models.Domain.Function4;
import models.Domain.Function5;
import nl.idgis.publisher.domain.query.GetGroupStructure;
import nl.idgis.publisher.domain.query.ListEnvironments;
import nl.idgis.publisher.domain.query.ListLayers;
import nl.idgis.publisher.domain.query.ListServiceKeywords;
import nl.idgis.publisher.domain.query.ListServices;
import nl.idgis.publisher.domain.query.PutGroupStructure;
import nl.idgis.publisher.domain.query.PutServiceKeywords;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.Layer;
import nl.idgis.publisher.domain.web.LayerGroup;
import nl.idgis.publisher.domain.web.Service;
import nl.idgis.publisher.domain.web.ServicePublish;
import nl.idgis.publisher.domain.web.tree.GroupLayer;
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
import views.html.services.form;
import views.html.services.list;
import views.html.services.publishService;
import actions.DefaultAuthenticator;
import akka.actor.ActorSelection;


@Security.Authenticated (DefaultAuthenticator.class)
public class Services extends Controller {

	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");
	private final static String ID="#CREATE_SERVICE#";
	
	private static Promise<Result> renderForm (final Form<ServiceForm> serviceForm, final GroupLayer groupLayer, final Boolean create) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		return from (database)
				.list (LayerGroup.class)
				.query (new ListLayers (1l, null, null))
				.execute (new Function2<Page<LayerGroup>,  Page<Layer>, Result> () {

					@Override
					public Result apply (final Page<LayerGroup> groups, final Page<Layer> layers) throws Throwable {
						return ok (form.render (serviceForm, create, groups, layers, groupLayer));
					}
				});
	}
	
	private static Promise<Result> renderPublishForm (final String serviceFormName, final Form<ServicePublishForm> servicePublishForm) {
		return Promise.promise(new F.Function0<Result>() {
             @Override
             public Result apply() throws Throwable {
            	 return ok (publishService.render (serviceFormName, servicePublishForm));
             }
        });
	}
	
	private static Promise<Result> renderCreateForm (final Form<ServiceForm> serviceForm) {
		return renderForm(serviceForm, null, true);
	}
	
	private static Promise<Result> renderEditForm (final Form<ServiceForm> serviceForm, final GroupLayer groupLayer) {
		return renderForm(serviceForm, groupLayer, false);
	}
	
	private static Promise<Result> renderCreatePublishForm (final String serviceFormName, final Form<ServicePublishForm> servicePublishForm) {
		return renderPublishForm(serviceFormName, servicePublishForm);
	}
	
	public static Promise<Result> submitCreate () {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		return from (database)
			.list (LayerGroup.class)
			.list (Layer.class)
			.list (Service.class)
			.executeFlat (new Function3<Page<LayerGroup>, Page<Layer>, Page<Service>, Promise<Result>> () {
	
				@Override
				public Promise<Result> apply (final Page<LayerGroup> groups, final Page<Layer> layers, final Page<Service> services) throws Throwable {
					final Form<ServiceForm> form = Form.form (ServiceForm.class).bindFromRequest ();
					Logger.debug ("CREATE Service: " + form.field("name").value());
					Logger.debug ("Form: "+ form);
					
					// validation start
					// check if the choosen service name already exists
					if (ID.equals(form.field("rootGroupId").value())){
						for (LayerGroup layerGroup : groups.values()) {
							if (form.field("name").value().trim ().equals(layerGroup.name())){
								form.reject("name", Domain.message("web.application.page.services.form.field.name.validation.groupexists.error"));
							}
						}
						for (Layer layer : layers.values()) {
							if (form.field("name").value().trim ().equals(layer.name())){
								form.reject("name", Domain.message("web.application.page.services.form.field.name.validation.layerexists.error"));
							}
						}
						for (Service service: services.values ()) {
							if (form.field ("name").value ().trim ().equals (service.name ())) {
								form.reject ("name", Domain.message("web.application.page.services.form.field.name.validation.serviceexists.error"));
							}
						}
					}
					if (form.hasErrors ()) {
						return renderCreateForm (form);
					}
					// validation end
					
					final ServiceForm serviceForm = form.get ();
					final Service service = new Service(ID, serviceForm.name, serviceForm.title, 
							serviceForm.alternateTitle,serviceForm.abstractText,
							serviceForm.metadata, serviceForm.published,
							serviceForm.rootGroupId,serviceForm.constantsId, false);
					Logger.debug ("Update/create service: " + service);
					
					final List<String> layerIds = (serviceForm.structure == null)?(new ArrayList<String>()):(serviceForm.structure);			
					Logger.debug ("Service rootgroup " + serviceForm.rootGroupId + " structure list: " + layerIds);
					
					final List<String> layerStyleIds = serviceForm.styles == null ? new ArrayList<>() : serviceForm.styles;

					return from (database)
						.put(service)
						.executeFlat (new Function<Response<?>, Promise<Result>> () {
							@Override
							public Promise<Result> apply (final Response<?> responseService) throws Throwable {
								String msg;
								// Get the id of the service we just put 
								String serviceId = responseService.getValue().toString();
								Logger.debug("serviceId: " + serviceId);
								PutServiceKeywords putServiceKeywords = 
										new PutServiceKeywords (serviceId, serviceForm.getKeywords()==null?new ArrayList<String>():serviceForm.getKeywords());
								
								PutGroupStructure putGroupStructure = new PutGroupStructure (serviceId, layerIds, layerStyleIds);
								return from (database)
									.query(putServiceKeywords)
									.query(putGroupStructure)
									.executeFlat (new Function2<Response<?>, Response<?>, Promise<Result>> () {
										@Override
										public Promise<Result> apply (final Response<?> responseKeywords, final Response<?> responseStructure) throws Throwable {
											// Check if the structure is valid i.e. does not contain cycles
											return from (database)
													.query (new GetGroupStructure(serviceId))
													.executeFlat (new Function<GroupLayer, Promise<Result>> () {
														@Override
														public Promise<Result>  apply (final GroupLayer groupLayer) throws Throwable {
															serviceForm.rootGroupId = serviceId;
															final Form<ServiceForm> formServiceForm = Form
																	.form (ServiceForm.class)
																	.fill (serviceForm);
															Logger.debug ("groupLayer: " + groupLayer);
															if (groupLayer==null){
																// CYCLE!!
																formServiceForm.reject("structure", Domain.message("web.application.page.services.form.field.structure.validation.cycle"));
															}
															if (formServiceForm.hasErrors ()) {
																
																return renderCreateForm (formServiceForm);
															} else {
																String msg;
																if (CrudOperation.CREATE.equals (responseService.getOperation())) {
																	msg = Domain.message("web.application.added").toLowerCase();
																}else{
																	msg = Domain.message("web.application.updated").toLowerCase();
																}									
																if (CrudResponse.OK.equals (responseService.getOperationResponse())) {
																	flash ("success", Domain.message("web.application.page.services.name") + " " + service.name() + " " + msg);
																}else{
																	flash ("danger", Domain.message("web.application.page.services.name") + " " + service.name() + " " + msg);
																}
																return Promise.pure (redirect (routes.Services.list (null, null, 1)));
															}
														}
												});
										}
									});
							}
						});
				}
			});
	}
	
	public static Promise<Result> submitUpdate (final String serviceIdentification) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		return from (database)
			.list (Service.class)
			.query (new GetGroupStructure(serviceIdentification))
			.executeFlat (new Function2<Page<Service>, GroupLayer, Promise<Result>> () {
	
				@Override
				public Promise<Result> apply (final Page<Service> services, final GroupLayer groupLayer) throws Throwable {
					final Form<ServiceForm> form = Form.form (ServiceForm.class).bindFromRequest ();
					Logger.debug ("UPDATE Service: " + form.field("name").value() + " ID: " + serviceIdentification);
					Logger.debug ("Form: "+ form);
					// validation start
					if (form.hasErrors ()) {
						return renderEditForm (form, groupLayer);
					}
					// validation end
					
					final ServiceForm serviceForm = form.get ();
					final Service service = new Service(serviceIdentification, serviceForm.name, serviceForm.title, 
							serviceForm.alternateTitle,serviceForm.abstractText,
							serviceForm.metadata, serviceForm.published,
							serviceIdentification,serviceForm.constantsId, false);
					Logger.debug ("Update service: " + service);
					
					final List<String> layerIds = (serviceForm.structure == null)?(new ArrayList<String>()):(serviceForm.structure);			
					Logger.debug ("Service rootgroup " + serviceForm.rootGroupId + " structure list: " + layerIds);
					
					final List<String> layerStyleIds = serviceForm.styles == null ? new ArrayList<>() : serviceForm.styles;

					return from (database)
						.put(service)
						.executeFlat (new Function<Response<?>, Promise<Result>> () {
							@Override
							public Promise<Result> apply (final Response<?> responseService) throws Throwable {
								String msg;
								// Get the id of the service we just put 
								String serviceId = responseService.getValue().toString();
								Logger.debug("serviceId: " + serviceId);
								PutServiceKeywords putServiceKeywords = 
										new PutServiceKeywords (serviceId, serviceForm.getKeywords()==null?new ArrayList<String>():serviceForm.getKeywords());
								
								PutGroupStructure putGroupStructure = new PutGroupStructure (serviceId, layerIds, layerStyleIds);
								return from (database)
									.query(putServiceKeywords)
									.query(putGroupStructure)
									.executeFlat (new Function2<Response<?>, Response<?>, Promise<Result>> () {
										@Override
										public Promise<Result> apply (final Response<?> responseKeywords, final Response<?> responseStructure) throws Throwable {
											// Check if the structure is valid i.e. does not contain cycles
											return from (database)
													.query (new GetGroupStructure(serviceId))
													.executeFlat (new Function<GroupLayer, Promise<Result>> () {
														@Override
														public Promise<Result>  apply (final GroupLayer groupLayer) throws Throwable {

															Logger.debug ("groupLayer: " + groupLayer);
															if (groupLayer==null){
																// CYCLE!!
																form.reject("structure", Domain.message("web.application.page.services.form.field.structure.validation.cycle"));
															}
															if (form.hasErrors ()) {
																return renderEditForm (form, groupLayer);
															} else {
																String msg;
																if (CrudOperation.CREATE.equals (responseService.getOperation())) {
																	msg = Domain.message("web.application.added").toLowerCase();
																}else{
																	msg = Domain.message("web.application.updated").toLowerCase();
																}									
																if (CrudResponse.OK.equals (responseService.getOperationResponse())) {
																	flash ("success", Domain.message("web.application.page.services.name") + " " + service.name() + " " + msg);
																}else{
																	flash ("danger", Domain.message("web.application.page.services.name") + " " + service.name() + " " + msg);
																}
																return Promise.pure (redirect (routes.Services.list (null, null, 1)));
															}
														}
												});
										}
									});
							}
						});
				}
			});
	}
	
	public static Promise<Result> list (final String query, final Boolean published, final long page) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);

		Logger.debug ("list Services ");
		
		return from (database)
			.query (new ListServices (page, query, published))
			.execute (new Function<Page<Service>, Result> () {
				@Override
				public Result apply (final Page<Service> services) throws Throwable {
					Logger.debug ("List Service: #" + services.values().size());
					return ok (list.render (services, query, published));
				}
			});
	}
	
	public static Promise<Result> publishService (final String serviceId, final long page) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		Logger.debug ("publish Service");
		
		return from (database)
			.get(Service.class,  serviceId)
			.query (new ListEnvironments (serviceId))
			.executeFlat (new Function2 <Service, Page<ServicePublish>, Promise<Result>> () {

				@Override
				public Promise<Result> apply (final Service service, final Page<ServicePublish> servicePublish) throws Throwable {
					Logger.debug("Service publish: " + servicePublish);
					final Form<ServicePublishForm> formServicePublishForm = Form
							.form (ServicePublishForm.class)
							.fill (new ServicePublishForm ());
					return renderCreatePublishForm (service.name(), formServicePublishForm);
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
			.query(new ListServiceKeywords(serviceId))
			.executeFlat (new Function2<Service, List<String>, Promise<Result>> () {

				@Override
				public Promise<Result> apply (final Service service, final List<String> keywords) throws Throwable {

					return from (database)
						.query (new GetGroupStructure(service.genericLayerId()))
						.executeFlat (new Function<GroupLayer, Promise<Result>> () {
							@Override
							public Promise<Result> apply (final GroupLayer groupLayer) throws Throwable {

								ServiceForm  serviceForm = new ServiceForm (service);
								serviceForm.setKeywords(keywords);
								Logger.debug ("Edit serviceForm: " + serviceForm);
								Logger.debug ("groupLayer: " + groupLayer);
								final Form<ServiceForm> formServiceForm = Form
										.form (ServiceForm.class)
										.fill (serviceForm);
								
//								return ok (form.render (formServiceForm, false, groups, layers, groupLayer, keywords));
								return renderEditForm (formServiceForm, groupLayer);
							}
					});
				}
			});
	}

	public static Promise<Result> delete(final String serviceId){
		Logger.debug ("delete Service " + serviceId);
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from(database).delete(Service.class, serviceId)
		.execute(new Function<Response<?>, Result>() {
			
			@Override
			public Result apply(Response<?> a) throws Throwable {
				return redirect (routes.Services.list (null, null, 1));
			}
		});
	}
	
	public static class ServicePublishForm {
		private String identification;
		private String name;
		private Boolean inUse = false;
		
		public ServicePublishForm () {
			super();
		}
		
		public ServicePublishForm (ServicePublish servicePublish){
			this.identification = servicePublish.identification();
			this.name = servicePublish.name();			
			this.inUse = servicePublish.guaranteedSV();
		}
		
		public String getIdentification() {
			return identification;
		}
		
		public void setIdentification(String identification) {
			this.identification = identification;
		}
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public Boolean getInUse() {
			return inUse;
		}
		
		public void setInUse(Boolean inUse) {
			this.inUse = inUse;
		}


	}
	
	
	public static class ServiceForm {

		@Constraints.Required (message = "web.application.page.services.form.field.name.validation.required")
		@Constraints.MinLength (value = 3, message = "web.application.page.services.form.field.name.validation.length")
		@Constraints.Pattern (value = "^[a-zA-Z0-9\\-\\_]+$", message = "web.application.page.services.form.field.name.validation.error")
		private String name;

		private String title;
		private String alternateTitle;
		private String abstractText;
		private List<String> keywords;
		private String metadata;
		private String watermark;
		private Boolean published = false;
		private String rootGroupId = "";
		private String constantsId = "";
		/**
		 * List of id's of layers/groups in this service
		 */
		private List<String> structure;

		
		/**
		 * List of id's of layer styles in this service
		 */
		private List<String> styles;
		
		public ServiceForm (){
			super();
			this.rootGroupId = ID;
			this.keywords = new ArrayList<String>();
		}
		
		public ServiceForm (final Service service){
			this.name = service.name();
			this.title = service.title();
			this.alternateTitle = service.alternateTitle();
			this.abstractText = service.abstractText();
			this.metadata = service.metadata();
			this.published = service.published();
			this.rootGroupId =service.genericLayerId();
			this.constantsId =service.constantsId();
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


		public List<String> getKeywords() {
			return keywords;
		}


		public void setKeywords(List<String> keywords) {
			this.keywords = keywords;
		}


		public String getMetadata() {
			return metadata;
		}


		public void setMetadata(String metadata) {
			this.metadata = metadata;
		}


		public String getRootGroupId() {
			return rootGroupId;
		}

		public void setRootGroupId(String rootGroupId) {
			this.rootGroupId = rootGroupId;
		}

		public String getConstantsId() {
			return constantsId;
		}

		public void setConstantsId(String constantsId) {
			this.constantsId = constantsId;
		}

		public String getWatermark() {
			return watermark;
		}


		public void setWatermark(String watermark) {
			this.watermark = watermark;
		}

		public Boolean getPublished() {
			return published;
		}

		public void setPublished(Boolean published) {
			this.published = published;
		}
		
		public List<String> getStructure() {
			return structure;
		}

		public void setStructure(List<String> structure) {
			this.structure = structure;
		}
		
		public List<String> getStyles() {
			return styles;
		}

		public void setStyles(List<String> styles) {
			this.styles = styles;
		}

		@Override
		public String toString() {
			return "ServiceForm [name=" + name + ", title="
					+ title + ", alternateTitle=" + alternateTitle
					+ ", abstractText=" + abstractText + ", keywords="
					+ keywords + ", metadata=" + metadata + ", watermark="
					+ watermark + ", published=" + published + ", rootGroupId="
					+ rootGroupId + ", constantsId=" + constantsId
					+ ", structure=" + structure + ", styles=" + styles + "]";
		}
		
		
		
	}
}

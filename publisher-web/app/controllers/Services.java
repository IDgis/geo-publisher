package controllers;

import static models.Domain.from;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import models.Domain;
import models.Domain.Function;
import models.Domain.Function2;
import models.Domain.Function3;
import models.Domain.Function4;

import nl.idgis.publisher.domain.query.GetGroupStructure;
import nl.idgis.publisher.domain.query.ListEnvironments;
import nl.idgis.publisher.domain.query.ListLayers;
import nl.idgis.publisher.domain.query.ListLdapUserGroups;
import nl.idgis.publisher.domain.query.ListServiceKeywords;
import nl.idgis.publisher.domain.query.ListServices;
import nl.idgis.publisher.domain.query.PerformPublish;
import nl.idgis.publisher.domain.query.PutGroupStructure;
import nl.idgis.publisher.domain.query.PutServiceKeywords;
import nl.idgis.publisher.domain.query.ValidateUniqueName;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.Constant;
import nl.idgis.publisher.domain.web.Layer;
import nl.idgis.publisher.domain.web.LayerGroup;
import nl.idgis.publisher.domain.web.LdapUserGroup;
import nl.idgis.publisher.domain.web.Service;
import nl.idgis.publisher.domain.web.ServicePublish;
import nl.idgis.publisher.domain.web.tree.GroupLayer;
import play.Logger;
import play.Play;
import play.data.Form;
import play.data.validation.Constraints;
import play.libs.Akka;
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
	
	private static Promise<Result> renderForm (final Form<ServiceForm> serviceForm, final GroupLayer groupLayer, final Boolean create, final Page<ServicePublish> servicePublish) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from (database)
				.list (LayerGroup.class)
				.query (new ListLayers (1l, null, null))
				.query (new ListLdapUserGroups (1L, "", true))
				.execute (new Function3<Page<LayerGroup>,  Page<Layer>, Page<LdapUserGroup>, Result> () {

					@Override
					public Result apply (final Page<LayerGroup> groups, final Page<Layer> layers, final Page<LdapUserGroup> allUserGroups) throws Throwable {
						return ok (form.render (serviceForm, create, groups, layers, groupLayer, servicePublish, allUserGroups));
					}
				});
	}
	
	private static Promise<Result> renderCreateForm (final Form<ServiceForm> serviceForm) {
		return renderForm(serviceForm, null, true, null);
	}
	
	private static Promise<Result> renderEditForm (final Form<ServiceForm> serviceForm, final GroupLayer groupLayer, final Page<ServicePublish> servicePublish) {
		return renderForm(serviceForm, groupLayer, false, servicePublish);
	}
	
	public static Promise<Result> submitCreate () {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		final Form<ServiceForm> form = Form.form (ServiceForm.class).bindFromRequest ();
		final String name = form.field ("name").valueOr (null);
		
		if(name == null) {
			return performCreate(database, form);
		} else {
			return from (database)
				.query (new ValidateUniqueName (name))
				.executeFlat (validationResult -> {
					if(validationResult.isValid ()) {
						Logger.debug("name is valid: " + name);
					} else {
						Logger.debug("name is already in use: " + name);
						
						switch(validationResult.conflictType ()) {
							case LAYER:
								form.reject ("name", "web.application.page.services.form.field.name.validation.layerexists.error");
								break;
							case LAYERGROUP:
								form.reject ("name", "web.application.page.services.form.field.name.validation.groupexists.error");
								break;
							case SERVICE:
								form.reject ("name", "web.application.page.services.form.field.name.validation.serviceexists.error");
								break;
							default:
								break;						
						}
					}
					
					return performCreate (database, form);
				});
		}
	}
	
	public static Promise<Result> submitUpdate (final String serviceIdentification) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		return from (database)
			.list (Service.class)
			.query (new GetGroupStructure(serviceIdentification))
			.query (new ListEnvironments (serviceIdentification))
			.executeFlat (new Function3<Page<Service>, GroupLayer, Page<ServicePublish>, Promise<Result>> () {
	
				@Override
				public Promise<Result> apply (final Page<Service> services, final GroupLayer groupLayer, final Page<ServicePublish> servicePublish) throws Throwable {
					final Form<ServiceForm> form = Form.form (ServiceForm.class).bindFromRequest ();
					Logger.debug ("UPDATE Service: " + form.field("name").value() + " ID: " + serviceIdentification);
					Logger.debug ("Form: "+ form);
					// validation start
					if (form.hasErrors ()) {
						return renderEditForm (form, groupLayer, servicePublish);
					}
					// validation end
					
					final ServiceForm serviceForm = form.get ();
					final Service service = new Service(
							serviceIdentification, serviceForm.name, serviceForm.title, 
							serviceForm.alternateTitle, serviceForm.abstractText,
							serviceForm.getUserGroups(), serviceForm.metadata, serviceIdentification,
							serviceForm.constantsId, null, null, false, false, false);
					Logger.debug ("Update service: " + service);
					
					final List<String> layerIds = (serviceForm.structure == null)?(new ArrayList<String>()):(serviceForm.structure);			
					Logger.debug ("Service rootgroup " + serviceForm.rootGroupId + " structure list: " + layerIds);
					
					final List<String> layerStyleIds = serviceForm.styles == null ? new ArrayList<>() : serviceForm.styles;

					return from (database)
						.put(service)
						.executeFlat (new Function<Response<?>, Promise<Result>> () {
							@Override
							public Promise<Result> apply (final Response<?> responseService) throws Throwable {
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
																return renderEditForm (form, groupLayer, servicePublish);
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
	
	public static Promise<Result> list (final String query, final Boolean isPublished, final long page) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);

		Logger.debug ("list Services ");
		
		return from (database)
			.query (new ListServices (page, query, isPublished))
			.execute (new Function<Page<Service>, Result> () {
				@Override
				public Result apply (final Page<Service> services) throws Throwable {
					Logger.debug ("List Service: #" + services.values().size());
					return ok (list.render (services, query, isPublished));
				}
			});
	}
	
	public static Promise<Result> submitPublishService(String serviceId) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		PerformPublish msg;
		Set<String> environmentIds = new HashSet<>(request().body().asFormUrlEncoded().keySet());
		Iterator<String> itr = environmentIds.iterator();
		if(itr.hasNext()) {
			msg = new PerformPublish(serviceId, itr.next());
			if(itr.hasNext()) {
				throw new IllegalArgumentException("multiple environments");
			}
		} else {
			msg = new PerformPublish(serviceId);
		}
		
		return from(database)
			.query(msg)
			.executeFlat(new Function <Boolean, Promise<Result>> () {
				@Override
				public Promise<Result> apply (final Boolean result) throws Throwable {
					if (result) {
						flash ("success", Domain.message("web.application.page.services.list.environments.success"));
					} else{
						flash ("danger", Domain.message("web.application.page.services.list.environments.failure"));
					}
					
					return Promise.pure (redirect (routes.Services.list (null, null, 1)));
				}
			});
	}
	
	public static Promise<Result> publishService (final String serviceId, final long page) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		Logger.debug ("publish Service");
		
		return from (database)
			.get(Service.class,  serviceId)
			.query (new ListEnvironments (serviceId))
			.query (new ListServiceKeywords (serviceId))
			.list (Constant.class)
			.execute (new Function4 <Service, Page<ServicePublish>, List<String>, Page<Constant>, Result> () {

				@Override
				public Result apply (final Service service, final Page<ServicePublish> servicePublish, final List<String> keywords, final Page<Constant> constants) throws Throwable {
					
					// Check preconditions for publishing this service, the following fields must have a value (non-null, non-empty):
					// - title
					// - alternative title
					// - abstract
					// - keywords
					boolean canPublish = true;
					final List<String> missingFields = new ArrayList<> ();
					if (isEmpty (service.title ())) {
						canPublish = false;
						missingFields.add ("web.application.page.services.form.field.title.label");
					}
					if (isEmpty (service.alternateTitle ())) {
						canPublish = false;
						missingFields.add ("web.application.page.services.form.field.alttitle.label");
					}
					if (isEmpty (service.abstractText ())) {
						canPublish = false;
						missingFields.add ("web.application.page.services.form.field.abstract.label");
					}
					if (keywords == null || keywords.isEmpty ()) {
						canPublish = false;
						missingFields.add ("web.application.page.services.form.field.keywords.label");
					}
					if (constants.values().isEmpty ()) {
						canPublish = false;
						missingFields.add ("web.application.layout.sidebar.constants");
					} else {
						final Constant constant = constants.values ().get (0);
						
						if (
								isEmpty (constant.contact ())
								|| isEmpty (constant.organization ())
								|| isEmpty (constant.position())
								|| isEmpty (constant.addressType())
								|| isEmpty (constant.address())
								|| isEmpty (constant.city())
								|| isEmpty (constant.state())
								|| isEmpty (constant.zipcode())
								|| isEmpty (constant.country())
								|| isEmpty (constant.telephone())
								|| isEmpty (constant.fax())
								|| isEmpty (constant.email())) {
							canPublish = false;
							missingFields.add ("web.application.layout.sidebar.constants");
						}
					}
					
					Logger.debug("Service publish: " + servicePublish);
					for (ServicePublish sp : servicePublish.values()) {
						Logger.debug("SP: " + sp.toString());
					}
					
					return ok(publishService.render(serviceId, service, servicePublish, canPublish, missingFields));
				}
			});
	}
	
	private static boolean isEmpty (final String value) {
		return value == null || value.trim ().isEmpty ();
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
			.query (new ListEnvironments (serviceId))
			.executeFlat (new Function3<Service, List<String>, Page<ServicePublish>, Promise<Result>> () {

				@Override
				public Promise<Result> apply (final Service service, final List<String> keywords, final Page<ServicePublish> servicePublish) throws Throwable {

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
								
								//return ok (form.render (formServiceForm, false, groups, layers, groupLayer, keywords));
								return renderEditForm (formServiceForm, groupLayer, servicePublish);
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
	
	private static Promise<Result> performCreate(final ActorSelection database, final Form<ServiceForm> form) {
		if (form.hasErrors ()) {
			return renderCreateForm (form);
		}
		// validation end
		
		final ServiceForm serviceForm = form.get ();
		final Service service = new Service(
				ID, serviceForm.name, serviceForm.title, 
				serviceForm.alternateTitle,serviceForm.abstractText,
				serviceForm.userGroups, serviceForm.metadata, 
				serviceForm.rootGroupId,serviceForm.constantsId, 
				null, null, false, false, false);
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

	public static class ServiceForm {

		@Constraints.Required (message = "web.application.page.services.form.field.name.validation.required")
		@Constraints.MinLength (value = 3, message = "web.application.page.services.form.field.name.validation.length")
		@Constraints.Pattern (value = "^[a-zA-Z0-9\\-\\_]+$", message = "web.application.page.services.form.field.name.validation.error")
		private String name;
		
		private String title;
		private String alternateTitle;
		private String abstractText;
		private List<String> keywords;
		private List<String> userGroups;
		private String metadata;
		private String watermark;
		private String rootGroupId = "";
		private String constantsId = "";
		private Boolean isPublished = false;
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
			this.userGroups = new ArrayList<String>();
		}
		
		public ServiceForm (final Service service){
			this.name = service.name();
			this.title = service.title();
			this.alternateTitle = service.alternateTitle();
			this.abstractText = service.abstractText();
			this.userGroups = service.userGroups();
			this.metadata = service.metadata();
			this.rootGroupId =service.genericLayerId();
			this.constantsId =service.constantsId();
			this.isPublished = service.isPublished();
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
			if (keywords==null){
				this.keywords = new ArrayList<String>();
			} else {
				this.keywords = keywords;
			}
		}
		
		public List<String> getUserGroups() {
			return userGroups;
		}
		
		public void setUserGroups(List<String> userGroups) {
			if (userGroups==null) {
				this.userGroups = new ArrayList<String>();
			} else {
				this.userGroups = userGroups;
			}
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
		
		public Boolean getIsPublished() {
			return isPublished;
		}
		
		public void setIsPublished(Boolean isPublished) {
			this.isPublished = isPublished;
		}
		
		public String getWatermark() {
			return watermark;
		}
		
		public void setWatermark(String watermark) {
			this.watermark = watermark;
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
			return "ServiceForm [name=" + name + ", title=" + title
					+ ", alternateTitle=" + alternateTitle + ", abstractText="
					+ abstractText + ", keywords=" + keywords + ", userGroups="
					+ userGroups + ", metadata=" + metadata + ", watermark="
					+ watermark + ", rootGroupId=" + rootGroupId 
					+ ", constantsId=" + constantsId + ", isPublished=" 
					+ isPublished + ", structure=" + structure + ", styles=" 
					+ styles + "]";
		}
	}
}

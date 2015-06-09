package controllers;

import static models.Domain.from;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;

import scala.runtime.AbstractFunction1;

import models.Domain;
import models.Domain.Function;
import models.Domain.Function2;
import models.Domain.Function3;
import models.Domain.Function5;

import nl.idgis.publisher.domain.query.GetGroupParentGroups;
import nl.idgis.publisher.domain.query.GetGroupParentServices;
import nl.idgis.publisher.domain.query.GetGroupStructure;
import nl.idgis.publisher.domain.query.GetGroupLayerRef;
import nl.idgis.publisher.domain.query.GetLayerServices;
import nl.idgis.publisher.domain.query.ListLayerGroups;
import nl.idgis.publisher.domain.query.ListLayers;
import nl.idgis.publisher.domain.query.PutGroupStructure;
import nl.idgis.publisher.domain.query.ValidateUniqueName;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.Layer;
import nl.idgis.publisher.domain.web.LayerGroup;
import nl.idgis.publisher.domain.web.Service;
import nl.idgis.publisher.domain.web.tree.GroupLayer;

import play.Logger;
import play.Play;
import play.api.mvc.Call;
import play.data.Form;
import play.data.validation.Constraints;
import play.libs.Akka;
import play.libs.Json;
import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Security;
import views.html.groups.form;
import views.html.groups.list;
import views.html.helper.groupStructureItem;
import views.html.layers.layerPagerBody;
import views.html.layers.layerPagerFooter;
import views.html.layers.layerPagerHeader;
import actions.DefaultAuthenticator;

import akka.actor.ActorSelection;

@Security.Authenticated (DefaultAuthenticator.class)
public class Groups extends GroupsLayersCommon {
	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");
	private final static String ID="#CREATE_GROUP#";
	
	private static Promise<Result> renderCreateForm (final Form<GroupForm> groupForm) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		return from (database)
			.query (new ListLayerGroups(1l, null))
			.query (new ListLayers (1l, null, null))
			.execute (new Function2<Page<LayerGroup>, Page<Layer>, Result> () {

				@Override
				public Result apply (final Page<LayerGroup> groups, final Page<Layer> layers) throws Throwable {
					return ok (form.render (groupForm, true, groups, layers, null, null, null, null));
				}
			});

	}
	
	private static Promise<Result> performCreateUpdate(final ActorSelection database, final Form<GroupForm> form) {
		if (form.field("structure").value() == null){
			form.reject("structure", Domain.message("web.application.page.groups.form.field.structure.validation.error"));
		}
		
		if (form.hasErrors ()) {
			return renderCreateForm (form);
		}
		// validation end
		
		final GroupForm groupForm = form.get ();
		final List<String> layerIds = (groupForm.structure == null)?(new ArrayList<String>()):(groupForm.structure);			
		Logger.debug ("Group structure list: " + layerIds);
		
		final List<String> layerStyleIds = groupForm.styles == null ? new ArrayList<>() : groupForm.styles;
		Logger.debug ("Group layer style list: " + layerStyleIds);
		
		final LayerGroup group = new LayerGroup(groupForm.id, groupForm.name.trim (), groupForm.title, 
				groupForm.abstractText,(groupForm.enabled ? groupForm.getTiledLayer() : null), false);
		
		return from (database)
			.put(group)
			.executeFlat (new Function<Response<?>, Promise<Result>> () {
				@Override
				public Promise<Result> apply (final Response<?> response) throws Throwable {
					// Get the id of the layer we just put 
					String groupId = response.getValue().toString();
					PutGroupStructure putGroupStructure = new PutGroupStructure (groupId, layerIds, layerStyleIds);															
					return from (database)
						.query(putGroupStructure)
						.executeFlat (new Function<Response<?>, Promise<Result>> () {
							@Override
							public Promise<Result> apply (final Response<?> response) throws Throwable {
								if (CrudOperation.CREATE.equals (response.getOperation())) {
									Logger.debug ("Created group " + group);
									if (response.getOperationResponse().equals(CrudResponse.NOK)){
										flash("danger", Domain.message("web.application.page.groups.form.field.structure.validation.cycle", response.getValue().toString()));
										form.reject("structure", Domain.message("web.application.page.groups.form.field.structure.validation.error"));
										return Promise.pure (redirect (routes.Groups.edit(groupId)));
									} else {
										flash ("success", Domain.message("web.application.page.groups.name") + " " + groupForm.getName () + " is " + Domain.message("web.application.added").toLowerCase());
									}
								}else{
									Logger.debug ("Updated group " + group);
									if (response.getOperationResponse().equals(CrudResponse.NOK)){
										flash("danger", Domain.message("web.application.page.groups.form.field.structure.validation.cycle", response.getValue().toString()));
										form.reject("structure", Domain.message("web.application.page.groups.form.field.structure.validation.error"));
										return Promise.pure (redirect (routes.Groups.edit(groupId)));
									} else {
										flash ("success", Domain.message("web.application.page.groups.name") + " " + groupForm.getName () + " is " + Domain.message("web.application.updated").toLowerCase());
									}
								}
								return Promise.pure (redirect (routes.Groups.list (null, 1)));
							}
						});
				}
			});
	}
	
	public static Promise<Result> submitCreateUpdate () {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		final Form<GroupForm> form = Form.form (GroupForm.class).bindFromRequest ();
		final String name = form.field ("name").valueOr (null);
		
		if (name == null) {
			return performCreateUpdate(database, form);
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
								form.reject ("name", "web.application.page.groups.form.field.name.validation.layerexists.error");
								break;
							case LAYERGROUP:
								form.reject ("name", "web.application.page.groups.form.field.name.validation.groupexists.error");
								break;
							case SERVICE:
								form.reject ("name", "web.application.page.groups.form.field.name.validation.serviceexists.error");
								break;
							default:
								break;						
						}
					}
					
					return performCreateUpdate (database, form);
				});
		}
	}
	
	public static Promise<Result> list (final String query, final long page) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);

		Logger.debug ("list Groups ");
		
		return from (database)
			.query (new ListLayerGroups (page, query))
			.execute (new Function<Page<LayerGroup>, Result> () {
				@Override
				public Result apply (final Page<LayerGroup> groups) throws Throwable {
					return ok (list.render (groups, query));
				}
			});
	}

	public static Promise<Result> create () {
		Logger.debug ("create Group");
		GroupForm groupForm = new GroupForm ();
		// set mimeformats to default
		groupForm.setMimeFormats(null);
		
		final Form<GroupForm> formGroupForm = Form.form (GroupForm.class).fill (groupForm);
		
		return renderCreateForm (formGroupForm);
	}
	
	public static Promise<Result> edit (final String groupId) {
		Logger.debug ("edit Group: " + groupId);
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from (database)
			.get (LayerGroup.class, groupId)
			.query (new GetGroupStructure(groupId))
			.query (new ListLayerGroups(1l, null))
			.query (new ListLayers (1l, null, null))
			.query(new GetLayerServices(groupId))
			.executeFlat (new Function5<LayerGroup, GroupLayer, Page<LayerGroup>, Page<Layer>, List<String>, Promise<Result>> () {

				@Override
				public Promise<Result> apply (final LayerGroup group, final GroupLayer groupLayer, final Page<LayerGroup> groups, final Page<Layer> layers, final List<String> serviceIds) throws Throwable {
					String serviceId;
					if (serviceIds==null || serviceIds.isEmpty()){
						serviceId="";
					} else {
						Logger.debug ("Services for group: " + group.name() + " # " + serviceIds.size());								
						// get the first service in the list for preview
						serviceId=serviceIds.get(0);
					}
					return from (database)
							.get(Service.class, serviceId)
							.query(new GetGroupParentGroups(groupId))
							.query(new GetGroupParentServices(groupId))
							.execute (new Function3<Service, Page<LayerGroup>, Page<Service>, Result> () {

							@Override
							public Result apply (final Service service, final Page<LayerGroup> parentGroups, final Page<Service> parentServices) throws Throwable {
									
								final Form<GroupForm> groupForm = Form
										.form (GroupForm.class)
										.fill (new GroupForm (group));
								
								Logger.debug ("Edit groupForm: " + groupForm);
								
								if(groupLayer==null){
									Logger.debug ("Group could not be edited: " + groupId);
									flash ("danger", 
										Domain.message("web.application.editing") + " " + 
										Domain.message("web.application.page.groups.name").toLowerCase() + " " + 
										Domain.message("web.application.failed").toLowerCase()
										+ " ("+Domain.message("web.application.page.groups.structure.error")+ ")");
									return redirect(routes.Groups.list (null, 1));
								}
								
								Logger.debug ("GROUP LAYER group name:" + groupLayer.getName() + " id:" + groupLayer.getId());
								for (nl.idgis.publisher.domain.web.tree.LayerRef layerRef : groupLayer.getLayers()) {
									nl.idgis.publisher.domain.web.tree.Layer layer = layerRef.getLayer();
									Logger.debug ("GROUP LAYER layer name:" + layer.getName() + " id:" + layer.getId());
								}
			
								// build a preview string
								final String previewUrl ;
								if (service==null){
									previewUrl = null;
								} else {
									previewUrl = makePreviewUrl(service.name(), group.name());
								}
								return ok (form.render (groupForm, false, groups, layers, groupLayer, parentGroups, parentServices, previewUrl));
							}
							});
				}
			});
	}

	public static Promise<Result> delete(final String groupId){
		Logger.debug ("delete Group " + groupId);
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from(database).delete(LayerGroup.class, groupId)
		.execute(new Function<Response<?>, Result>() {
			
			@Override
			public Result apply(Response<?> response) throws Throwable {
				if (response.getOperationResponse().equals(CrudResponse.NOK)) {
					Logger.debug ("Group could not be deleted: " + groupId);
					flash ("danger", 
						Domain.message("web.application.removing") + " " + 
						Domain.message("web.application.page.groups.name").toLowerCase() + " " + 
						Domain.message("web.application.failed").toLowerCase()
						+" ("+Domain.message("web.application.page.groups.remove.failed")+")");
				}else{
					Logger.debug ("Deleted group " + groupId);
					flash ("success", 
						Domain.message("web.application.removing") + " " + 
						Domain.message("web.application.page.groups.name").toLowerCase() + " " + 
						Domain.message("web.application.succeeded").toLowerCase()
						);
				}
				return redirect (routes.Groups.list (null, 1));
			}
		});
		
	}
	
	
	public static class GroupForm extends TiledLayerForm{

		@Constraints.Required
		private String id;
		@Constraints.Required (message = "web.application.page.groups.form.field.name.validation.required")
		@Constraints.MinLength (value = 3, message = "web.application.page.groups.form.field.name.validation.length")
		@Constraints.Pattern (value = "^[a-zA-Z][a-zA-Z0-9\\-\\_]+$", message = "web.application.page.groups.form.field.name.validation.error")
		private String name;
		private String title;
		private String abstractText;
		/**
		 * List of id's of layers/groups in this group
		 */
		private List<String> structure;
		
		/**
		 * List of id's of styles in this group
		 */
		private List<String> styles;

		private Boolean enabled = false;

		public GroupForm(){
			super();
			this.id=ID;
		}
		
		public GroupForm(LayerGroup group){
			super(group.tiledLayer().isPresent()?group.tiledLayer().get():null);
			this.id = group.id();
			this.name = group.name();
			this.title = group.title();
			this.abstractText = group.abstractText();
			this.enabled = group.tiledLayer().isPresent();

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

		public String getAbstractText() {
			return abstractText;
		}

		public void setAbstractText(String abstractText) {
			this.abstractText = abstractText;
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
		
		public Boolean getEnabled() {
			return enabled;
		}

		public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}

	}
	
	public static Promise<Result> structureItem(String groupId, boolean showStyleSelect) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from(database)
			.query(new GetGroupLayerRef(groupId))
			.execute(groupLayerRef ->
				ok(groupStructureItem.render(groupLayerRef, showStyleSelect)));
	}
	
	public static Promise<Result> listJson (final String query, final long page) {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);

		return from (database)
			.query (new ListLayerGroups (page, query))
			.execute (new Function<Page<LayerGroup>, Result> () {
				@Override
				public Result apply (final Page<LayerGroup> layerGroups) throws Throwable {
					final ObjectNode result = Json.newObject ();
					
					result.put ("header", layerPagerHeader.render (query).toString ());
					result.put ("body", layerPagerBody.render (layerGroups).toString ());
					result.put ("footer", layerPagerFooter.render (layerGroups, new AbstractFunction1<Long, Call>() {

						@Override
						public Call apply(Long page) {
							return routes.Groups.listJson(query, page);
						}
						
					}).toString ());

					return ok (result);
				}
			});
	}
	
}
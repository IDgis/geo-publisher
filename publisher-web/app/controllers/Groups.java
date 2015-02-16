package controllers;

import static models.Domain.from;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import controllers.Groups.GroupForm;
import models.Domain;
import models.Domain.Function;
import models.Domain.Function2;
import models.Domain.Function3;
import models.Domain.Function4;
import nl.idgis.publisher.domain.query.GetGroupStructure;
import nl.idgis.publisher.domain.query.PutGroupStructure;
import nl.idgis.publisher.domain.query.PutLayerStyles;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.Layer;
import nl.idgis.publisher.domain.web.LayerGroup;
import nl.idgis.publisher.domain.web.Style;
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
import views.html.groups.list;
import views.html.groups.form;
import actions.DefaultAuthenticator;
import akka.actor.ActorSelection;

@Security.Authenticated (DefaultAuthenticator.class)
public class Groups extends Controller {
	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");

	private static Promise<Result> renderCreateForm (final Form<GroupForm> groupForm) {
			final ActorSelection database = Akka.system().actorSelection (databaseRef);
			return from (database)
					.list (LayerGroup.class)
					.list (Layer.class)
					.execute (new Function2<Page<LayerGroup>, Page<Layer>, Result> () {

						@Override
						public Result apply (final Page<LayerGroup> groups, final Page<Layer> layers) throws Throwable {
							return ok (form.render (groupForm, true, groups, layers, null));
						}
					});

	}
	
	public static Promise<Result> submitCreateUpdate () {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		final Form<GroupForm> form = Form.form (GroupForm.class).bindFromRequest ();
		Logger.debug ("submit Group: " + form.field("name").value());
		
		// validation
		if (form.field("name").value().length() == 1 ) 
			form.reject("name", Domain.message("web.application.page.groups.form.field.name.validation.error", "1"));
		if (form.hasErrors ()) {
			return renderCreateForm (form);
		}
		
		final GroupForm groupForm = form.get ();
		
		List<String> structureIds;
		if (groupForm.structure == null){
			// empty list
			structureIds = new ArrayList<String>();
		} else {
			structureIds = groupForm.structure;
		}
		
		final List<String> layerIds = structureIds;
		Logger.debug ("Group structure: " + groupForm.structure + ", list: " + layerIds);
		
		final LayerGroup group = new LayerGroup(groupForm.id, groupForm.name, groupForm.title, 
				groupForm.abstractText,groupForm.published);
		
		return from (database)
			.put(group)
			.executeFlat (new Function<Response<?>, Promise<Result>> () {
				@Override
				public Promise<Result> apply (final Response<?> response) throws Throwable {
					// Get the id of the layer we just put 
					String groupId = response.getValue().toString();
					PutGroupStructure putGroupStructure = new PutGroupStructure (groupId, layerIds);															
					return from (database)
						.query(putGroupStructure)
						.executeFlat (new Function<Response<?>, Promise<Result>> () {
							@Override
							public Promise<Result> apply (final Response<?> response) throws Throwable {
								if (CrudOperation.CREATE.equals (response.getOperation())) {
									Logger.debug ("Created group " + group);
									flash ("success", Domain.message("web.application.page.groups.name") + " " + groupForm.getName () + " is " + Domain.message("web.application.added").toLowerCase());
								}else{
									Logger.debug ("Updated group " + group);
									flash ("success", Domain.message("web.application.page.groups.name") + " " + groupForm.getName () + " is " + Domain.message("web.application.updated").toLowerCase());
								}
								return Promise.pure (redirect (routes.Groups.list ()));
							}
						});
				}
			});

	}
	
	public static Promise<Result> list () {
		final ActorSelection database = Akka.system().actorSelection (databaseRef);

		Logger.debug ("list Groups ");
		
		return from (database)
			.list (LayerGroup.class)
			.execute (new Function<Page<LayerGroup>, Result> () {
				@Override
				public Result apply (final Page<LayerGroup> groups) throws Throwable {
					return ok (list.render (groups));
				}
			});
	}

	public static Promise<Result> create () {
		Logger.debug ("create Group");
		final Form<GroupForm> groupForm = Form.form (GroupForm.class).fill (new GroupForm ());
		
		return renderCreateForm (groupForm);
	}
	
	public static Promise<Result> edit (final String groupId) {
		Logger.debug ("edit Group: " + groupId);
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		return from (database)
			.get (LayerGroup.class, groupId)
			.query (new GetGroupStructure(groupId))
			.list (LayerGroup.class)
			.list (Layer.class)
			.execute (new Function4<LayerGroup, GroupLayer, Page<LayerGroup>, Page<Layer>, Result> () {

				@Override
				public Result apply (final LayerGroup group, final GroupLayer groupLayer, final Page<LayerGroup> groups, final Page<Layer> layers) throws Throwable {
					final Form<GroupForm> groupForm = Form
							.form (GroupForm.class)
							.fill (new GroupForm (group));
					
					Logger.debug ("Edit groupForm: " + groupForm);		
					Logger.debug ("GROUP LAYER layer name:" + groupLayer.getName() + " id:" + groupLayer.getId());
					for (nl.idgis.publisher.domain.web.tree.Layer layer : groupLayer.getLayers()) {
						Logger.debug ("GROUP LAYER layer name:" + layer.getName() + " id:" + layer.getId());
					}

					return ok (form.render (groupForm, false, groups, layers, groupLayer));
				}
			});
	}

	public static Promise<Result> delete(final String groupId){
		Logger.debug ("delete Group " + groupId);
		final ActorSelection database = Akka.system().actorSelection (databaseRef);
		
		from(database).delete(LayerGroup.class, groupId)
		.execute(new Function<Response<?>, Result>() {
			
			@Override
			public Result apply(Response<?> a) throws Throwable {
				return redirect (routes.Groups.list ());
			}
		});
		
		return Promise.pure (redirect (routes.Groups.list ()));
	}
	
	
	public static class GroupForm{

		@Constraints.Required
		private String id;
		private String name;
		private String title;
		private String abstractText;
		private Boolean published = false;
		/**
		 * List of id's of layers/groups in this group
		 */
		private List<String> structure;

		public GroupForm(){
			super();
			this.id = UUID.randomUUID().toString();
		}
		
		public GroupForm(LayerGroup group){
			this.id = group.id();
			this.name = group.name();
			this.title = group.title();
			this.abstractText = group.abstractText();
			this.published = group.published();

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

	}
	
}
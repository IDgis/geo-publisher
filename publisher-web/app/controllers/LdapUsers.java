package controllers;

import static models.Domain.from;

import java.util.List;

import actions.DefaultAuthenticator;
import akka.actor.ActorSelection;
import controllers.Groups.GroupForm;
import models.Domain;
import models.Domain.Function;
import models.Domain.Function3;
import models.Domain.Function5;
import nl.idgis.publisher.domain.query.GetGroupParentGroups;
import nl.idgis.publisher.domain.query.GetGroupParentServices;
import nl.idgis.publisher.domain.query.GetGroupStructure;
import nl.idgis.publisher.domain.query.GetLayerServices;
import nl.idgis.publisher.domain.query.ListLayerGroups;
import nl.idgis.publisher.domain.query.ListLayers;
import nl.idgis.publisher.domain.query.ListLdapUsers;
import nl.idgis.publisher.domain.query.ValidateUniqueName;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.Layer;
import nl.idgis.publisher.domain.web.LayerGroup;
import nl.idgis.publisher.domain.web.LdapUser;
import nl.idgis.publisher.domain.web.Service;
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

@Security.Authenticated(DefaultAuthenticator.class)
public class LdapUsers extends Controller {
	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");
	
	public static Promise<Result> list(final String query, final long page) {
		Logger.debug("list LdapUsers");
		final ActorSelection database = Akka.system().actorSelection(databaseRef);
		
		return from(database)
			.query(new ListLdapUsers(page, query, false))
			.execute(new Function<Page<LdapUser>, Result>() {
				@Override
				public Result apply(final Page<LdapUser> users) throws Throwable {
					return ok(views.html.ldap.users.list.render(users, query));
				}
			});
	}
	
	public static Promise<Result> create() {
		Logger.debug("create LdapUser");
		final Form<UserForm> userForm = Form.form(UserForm.class).fill(new UserForm());
		
		return Promise.pure(ok(views.html.ldap.users.form.render(userForm, true)));
	}
	
	public static Promise<Result> edit(final String email) {
		Logger.debug("edit LdapUser: " + email);
		final ActorSelection database = Akka.system().actorSelection(databaseRef);
		
		return from(database)
			.get(LdapUser.class, email)
			.executeFlat(new Function<LdapUser, Promise<Result>>() {
				
				@Override
				public Promise<Result> apply(final LdapUser ldapUser) throws Throwable {
					final Form<UserForm> userForm = Form
							.form(UserForm.class)
							.fill(new UserForm(ldapUser));
					
					return Promise.pure(ok(views.html.ldap.users.form.render(userForm, false)));
				}
			});
	}
	
	public static Promise<Result> performCreateUpdate(boolean create) {
		Logger.debug("performing create/update LdapUser");
		final ActorSelection database = Akka.system().actorSelection(databaseRef);
		
		final Form<UserForm> form = Form.form(UserForm.class).bindFromRequest();
		
		if(form.hasErrors()) {
			Logger.debug("LdapUserForm errors " + form.errorsAsJson().toString());
			return Promise.pure(ok(views.html.ldap.users.form.render(form, create)));
		}
		
		UserForm userForm = form.get();
		
		LdapUser user = 
			new LdapUser(
					null, 
					userForm.getEmail().trim(), 
					userForm.getFullName(), 
					userForm.getLastName(), 
					userForm.getPassword());
		
		return from(database)
				.put(user)
				.execute(new Function<Response<?>, Result>() {
					
					@Override
					public Result apply(Response<?> response) throws Throwable {
						if(CrudResponse.OK.equals(response.getOperationResponse())) {
							Logger.debug("Created/updated LdapUser successful: " + userForm.getEmail());
						} else {
							Logger.debug("Created/updated LdapUser failed: " + userForm.getEmail());
						}
						
						flash(
							CrudResponse.OK.equals(response.getOperationResponse()) ? "success" : "danger", 
							getOperationFromResponse(response) + 
							" " + 
							Domain.message("web.application.page.ldap.user.name").toLowerCase() + 
							" " + 
							userForm.getEmail() + " is " + getResultFromResponse(response)
						);
						
						return redirect(routes.LdapUsers.list(null, 1));
					}
				});
	}
	
	public static Promise<Result> submitCreate() {
		return performCreateUpdate(true);
	}
	
	public static Promise<Result> submitUpdate() {
		return performCreateUpdate(false);
	}
	
	public static Promise<Result> delete(final String email) {
		Logger.debug("delete LdapUser " + email);
		final ActorSelection database = Akka.system().actorSelection(databaseRef);
		
		return from(database)
			.delete(LdapUser.class, email)
			.execute(new Function<Response<?>, Result>() {
				
				@Override
				public Result apply(Response<?> response) throws Throwable {
					flash(
						CrudResponse.OK.equals(response.getOperationResponse()) ? "success" : "danger", 
						Domain.message("web.application.removing") + " " + 
						Domain.message("web.application.page.ldap.user.name").toLowerCase() + 
						" " + email.trim() + " is " + 
						getResultFromResponse(response));
					
					return redirect(routes.LdapUsers.list(null, 1));
				}
			});
	}
	
	public static String getResultFromResponse(Response<?> response) {
		return CrudResponse.OK.equals(response.getOperationResponse()) ? 
			Domain.message("web.application.succeeded").toLowerCase() : 
			Domain.message("web.application.failed").toLowerCase();
	}
	
	public static String getOperationFromResponse(Response<?> response) {
		return CrudOperation.CREATE.equals(response.getOperation()) ? 
			Domain.message("web.application.adding") : 
			Domain.message("web.application.updating");
	}
	
	public static class UserForm {
		
		@Constraints.Required(message = "web.application.page.ldap.user.form.field.email.validation.required")
		@Constraints.Email(message = "web.application.page.ldap.user.form.field.email.validation.email")
		private String email;
		@Constraints.Required(message = "web.application.page.ldap.user.form.field.fullname.validation.required")
		private String fullName;
		@Constraints.Required(message = "web.application.page.ldap.user.form.field.lastname.validation.required")
		private String lastName;
		@Constraints.Required(message = "web.application.page.ldap.user.form.field.password.validation.required")
		private String password;
		
		public UserForm() {
			super();
		}
		
		public UserForm(LdapUser user){
			super();
			this.email = user.mail();
			this.fullName = user.fullName();
			this.lastName = user.lastName();
			this.password = user.password();
		}
		
		public String getEmail() {
			return email;
		}
		
		public void setEmail(String email) {
			this.email = email;
		}
		
		public String getFullName() {
			return fullName;
		}
		
		public void setFullName(String fullName) {
			this.fullName = fullName;
		}
		
		public String getLastName() {
			return lastName;
		}
		
		public void setLastName(String lastName) {
			this.lastName = lastName;
		}
		
		public String getPassword() {
			return password;
		}
		
		public void setPassword(String password) {
			this.password = password;
		}
	}
}

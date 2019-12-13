package controllers;

import static models.Domain.from;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import actions.DefaultAuthenticator;
import akka.actor.ActorSelection;
import models.Domain;
import models.Domain.Function;
import models.Domain.Function2;
import nl.idgis.publisher.domain.query.ListLdapUserGroups;
import nl.idgis.publisher.domain.query.ListLdapUsers;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.LdapUser;
import nl.idgis.publisher.domain.web.LdapUserGroup;
import play.Logger;
import play.Play;
import play.data.Form;
import play.data.validation.Constraints;
import play.libs.Akka;
import play.libs.Json;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

@Security.Authenticated(DefaultAuthenticator.class)
public class LdapUserGroups extends Controller {
	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");
	private final static String ldapBaseDn = Play.application().configuration().getString("publisher.ldap.base.dn");
	
	public static Promise<Result> list(final String query, final long page) {
		Logger.debug("list LdapUserGroups");
		final ActorSelection database = Akka.system().actorSelection(databaseRef);
		
		return from(database)
			.query(new ListLdapUserGroups(page, query, false))
			.execute(new Function<Page<LdapUserGroup>, Result>() {
				@Override
				public Result apply(final Page<LdapUserGroup> userGroups) throws Throwable {
					return ok(views.html.ldap.usergroups.list.render(userGroups, query));
				}
			});
	}
	
	public static Promise<Result> create() {
		Logger.debug("create LdapUserGroup");
		final ActorSelection database = Akka.system().actorSelection(databaseRef);
		
		final Form<UserGroupForm> userGroupForm = Form.form(UserGroupForm.class).fill(new UserGroupForm());
		
		return from(database)
			.query(new ListLdapUsers(1L, "", true))
			.execute(new Function<Page<LdapUser>, Result>() {
				
				@Override
				public Result apply(final Page<LdapUser> users) throws Throwable {
					return ok(views.html.ldap.usergroups.form.render(userGroupForm, "[]", users, true));
				}
			});
	}
	
	public static Promise<Result> edit(final String name) {
		Logger.debug("edit LdapUserGroup: " + name);
		final ActorSelection database = Akka.system().actorSelection(databaseRef);
		
		return from(database)
			.get(LdapUserGroup.class, name)
			.query(new ListLdapUsers(1L, "", true))
			.execute(new Function2<LdapUserGroup, Page<LdapUser>, Result>() {
				
				@Override
				public Result apply(final LdapUserGroup ldapUserGroup, final Page<LdapUser> users) throws Throwable {
					final Form<UserGroupForm> userGroupForm = Form
							.form(UserGroupForm.class)
							.fill(new UserGroupForm(ldapUserGroup));
					
					return ok(views.html.ldap.usergroups.form.render(userGroupForm, ldapUserGroup.members().toString(), users, false));
				}
			});
	}
	
	public static Promise<Result> performCreateUpdate(boolean create) {
		Logger.debug("performing create/update LdapUserGroup");
		final ActorSelection database = Akka.system().actorSelection(databaseRef);
		
		final Form<UserGroupForm> form = Form.form(UserGroupForm.class).bindFromRequest();
		
		if(form.field("users").value().isEmpty() || form.field("users").value().equals("[]")) {
			Logger.debug("LdapUserGroup: Empty user list");
			form.reject("users", Domain.message("web.application.page.ldap.usergroup.form.field.users.validation.minimum"));
		} else {
			Logger.debug("LdapUserGroup: Form user list " + form.field("users").value());
		}
		
		if(form.hasErrors()) {
			return from(database)
				.query(new ListLdapUsers(1L, "", true))
				.execute(new Function<Page<LdapUser>, Result>() {
					
					@Override
					public Result apply(final Page<LdapUser> users) throws Throwable {
						Logger.debug("LdapUserGroupForm errors " + form.errorsAsJson().toString());
						return ok(views.html.ldap.usergroups.form.render(form, form.field("users").value(), users, create));
					}
				});
		}
		
		UserGroupForm userGroupForm = form.get();
		
		String userListString = userGroupForm.getUsers();
		
		final List<String> userList = new ArrayList<>();
		for(final JsonNode user: Json.parse(userListString)) {
			userList.add("mail=" + user.asText() + ",ou=users," + ldapBaseDn);
		}
		
		Logger.debug("LdapUserGroup user list: " + userList.toString());
		
		LdapUserGroup userGroup = new LdapUserGroup(null, userGroupForm.getName().trim(), userList);
		
		return from(database)
				.put(userGroup)
				.execute(new Function<Response<?>, Result>() {
					
					@Override
					public Result apply(Response<?> response) throws Throwable {
						if(CrudResponse.OK.equals(response.getOperationResponse())) {
							Logger.debug("Created/updated LdapUserGroup successful: " + userGroupForm.getName());
						} else {
							Logger.debug("Created/updated LdapUserGroup failed: " + userGroupForm.getName());
						}
						
						flash(
							CrudResponse.OK.equals(response.getOperationResponse()) ? "success" : "danger", 
							getOperationFromResponse(response) + 
							" " + 
							Domain.message("web.application.page.ldap.usergroup.name").toLowerCase() + 
							" " + 
							userGroupForm.getName() + " is " + getResultFromResponse(response)
						);
						
						return redirect(routes.LdapUserGroups.list(null, 1));
					}
				});
	}
	
	public static Promise<Result> submitCreate() {
		return performCreateUpdate(true);
	}
	
	public static Promise<Result> submitUpdate() {
		return performCreateUpdate(false);
	}
	
	public static Promise<Result> delete(final String name) {
		Logger.debug("delete LdapUserGroup " + name);
		final ActorSelection database = Akka.system().actorSelection(databaseRef);
		
		return from(database)
			.delete(LdapUserGroup.class, name)
			.execute(new Function<Response<?>, Result>() {
				
				@Override
				public Result apply(Response<?> response) throws Throwable {
					flash(
						CrudResponse.OK.equals(response.getOperationResponse()) ? "success" : "danger", 
						Domain.message("web.application.removing") + " " + 
						Domain.message("web.application.page.ldap.usergroup.name").toLowerCase() + 
						" " + name.trim() + " is " + 
						getResultFromResponse(response));
					
					return redirect(routes.LdapUserGroups.list(null, 1));
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
	
	public static class UserGroupForm {
		
		@Constraints.Required(message = "web.application.page.ldap.usergroup.form.field.name.validation.required")
		private String name;
		private String users;
		private List<String> userList;
		
		public UserGroupForm() {
			super();
		}
		
		public UserGroupForm(LdapUserGroup userGroup){
			super();
			
			this.name = userGroup.name();
			this.userList = userGroup.members();
		}
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public String getUsers() {
			return users;
		}
		
		public void setUsers(String users) {
			this.users = users;
		}
		
		public List<String> getUserList() {
			return userList;
		}
		
		public void setUserList(List<String> userList) {
			this.userList = userList;
		}
	}
}

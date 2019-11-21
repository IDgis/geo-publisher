package nl.idgis.publisher.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import akka.actor.ActorRef;
import akka.actor.Props;
import nl.idgis.publisher.domain.query.ListLdapUserGroups;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.LdapUserGroup;

public class LdapUserGroupAdmin extends AbstractLdapAdmin {
	
	public LdapUserGroupAdmin(ActorRef database, String ldapApiAdminMail, String ldapApiAdminPassword, String ldapApiAdminUrlBaseUsers, String ldapApiAdminUrlBaseOrganizations) {
		super(database, ldapApiAdminMail, ldapApiAdminPassword, ldapApiAdminUrlBaseUsers, ldapApiAdminUrlBaseOrganizations);
	}
	
	public static Props props(ActorRef database, String ldapApiAdminMail, String ldapApiAdminPassword, String ldapApiAdminUrlBaseUsers, String ldapApiAdminUrlBaseOrganizations) {
		return Props.create(LdapUserGroupAdmin.class, database, ldapApiAdminMail, ldapApiAdminPassword, ldapApiAdminUrlBaseUsers, ldapApiAdminUrlBaseOrganizations);
	}

	@Override
	protected void preStartAdmin() {
		doQuery(ListLdapUserGroups.class, this::handleListLdapUserGroups);
		doGet(LdapUserGroup.class, this::handleGetLdapUserGroup);
		doDelete(LdapUserGroup.class, this::handleDeleteLdapUserGroup);
		doPut(LdapUserGroup.class, this::handlePutLdapUserGroup);
	}
	
	private CompletableFuture<Page<LdapUserGroup>> handleListLdapUserGroups(final ListLdapUserGroups listLdapUserGroups) {
		log.debug("handleListLdapUserGroups");
		final Page.Builder<LdapUserGroup> builder = new Page.Builder<>();
		
		doGetRequest(ldapApiAdminUrlBaseOrganizations)
			.ifPresent(response -> {
				try {
					JsonArray jsonArray = new JsonParser().parse(response).getAsJsonArray();
					List<JsonObject> list = 
							sortJsonArray(jsonArray, "name")
								.stream()
								.skip((listLdapUserGroups.getPage() - 1) * DEFAULT_ITEMS_PER_PAGE)
								.limit(DEFAULT_ITEMS_PER_PAGE)
								.collect(Collectors.toList());
					
					addPageInfo(builder, listLdapUserGroups.getPage(), jsonArray.size());
					
					for(JsonObject object : list) {
						builder.add(new LdapUserGroup(
							null,
							object.get("name").getAsString(),
							null
						));
					}
				} catch(JsonParseException jpe) {
					log.debug("LDAP get usergroup list request: can't parse json of response");
				} catch(IllegalStateException ise) {
					log.debug("LDAP get usergroup list request: unexpected json response");
				} catch(Exception e) {
					log.debug("LDAP get usergroup list request: something went wrong");
				}
			});
		
		return CompletableFuture.supplyAsync(() -> builder.build());
	}
	
	private CompletableFuture<Optional<LdapUserGroup>> handleGetLdapUserGroup(String name) {
		log.debug("handleGetLdapUserGroup: {}", name);
		return CompletableFuture.supplyAsync(() -> getLdapUserGroup(name));
	}
	
	private CompletableFuture<Response<?>> handlePutLdapUserGroup(LdapUserGroup usergroup) {
		log.debug("handlePutLdapUserGroup: {}", usergroup.name());
		boolean emailExists = getLdapUserGroup(usergroup.name()).isPresent();
		CrudOperation operation = emailExists ? CrudOperation.UPDATE : CrudOperation.CREATE;
		
		try {
			doPostPutRequest(ldapApiAdminUrlBaseOrganizations, emailExists, new Gson().toJson(usergroup));
		} catch(Exception e) {
			log.debug("LDAP post/put usergroup request: something went wrong performing the request");
			
			return CompletableFuture.supplyAsync(() -> {
				return new Response<String>(operation, CrudResponse.NOK, usergroup.name());
			});
		}
		
		return CompletableFuture.supplyAsync(() -> {
			return new Response<String>(operation, CrudResponse.OK, usergroup.name());
		});
	}
	
	private CompletableFuture<Response<?>> handleDeleteLdapUserGroup(String name) {
		log.debug("handleDeleteLdapUserGroup: {}", name);
		
		try {
			doDeleteRequest(ldapApiAdminUrlBaseOrganizations + "/" + name);
		} catch(Exception e) {
			log.debug("LDAP delete usergroup request: something went wrong performing the request");
			
			return CompletableFuture.supplyAsync(() -> {
				return new Response<String>(CrudOperation.DELETE, CrudResponse.NOK, name);
			});
		}
		
		return CompletableFuture.supplyAsync(() -> {
			return new Response<String>(CrudOperation.DELETE, CrudResponse.OK, name);
		});
	}
	
	private Optional<LdapUserGroup> getLdapUserGroup(String name) {
		log.debug("getLdapUserGroup: {}", name);
		
		return
			doGetRequest(ldapApiAdminUrlBaseOrganizations + "/" + name)
				.flatMap(response -> {
					try {
						JsonObject object = new JsonParser().parse(response).getAsJsonObject();
						JsonArray members = object.get("members").getAsJsonArray();
						
						List<String> listMembers = new ArrayList<>();
						String mailKey = "mail=";
						members.forEach(item -> {
							String member = item.getAsString();
							
							String[] memberEntities = member.split(",");
							String mailEntity = memberEntities[0];
							String mail = mailEntity.substring(mailEntity.indexOf(mailKey) + mailKey.length());
							
							listMembers.add("\"" + mail + "\"");
						});
						
						return Optional.of(
								new LdapUserGroup(
										null, object.get("name").getAsString(), sortStringList(listMembers)));
					} catch(JsonParseException jpe) {
						log.debug("LDAP get usergroup request: can't parse json of response");
					} catch(IllegalStateException ise) {
						log.debug("LDAP get usergroup request: unexpected json response");
					} catch(Exception e) {
						log.debug("LDAP get usergroup request: something went wrong");
					}
					
					return Optional.empty();
				});
	}
}

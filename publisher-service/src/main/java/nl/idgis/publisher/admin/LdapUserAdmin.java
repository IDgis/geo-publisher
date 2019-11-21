package nl.idgis.publisher.admin;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import akka.actor.ActorRef;
import akka.actor.Props;
import nl.idgis.publisher.domain.query.ListLdapUsers;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.LdapUser;

public class LdapUserAdmin extends AbstractLdapAdmin {
	
	public LdapUserAdmin(ActorRef database, String ldapApiAdminMail, String ldapApiAdminPassword, String ldapApiAdminUrlBaseUsers, String ldapApiAdminUrlBaseOrganizations) {
		super(database, ldapApiAdminMail, ldapApiAdminPassword, ldapApiAdminUrlBaseUsers, ldapApiAdminUrlBaseOrganizations);
	}
	
	public static Props props(ActorRef database, String ldapApiAdminMail, String ldapApiAdminPassword, String ldapApiAdminUrlBaseUsers, String ldapApiAdminUrlBaseOrganizations) {
		return Props.create(LdapUserAdmin.class, database, ldapApiAdminMail, ldapApiAdminPassword, ldapApiAdminUrlBaseUsers, ldapApiAdminUrlBaseOrganizations);
	}

	@Override
	protected void preStartAdmin() {
		doQuery(ListLdapUsers.class, this::handleListLdapUsers);
		doGet(LdapUser.class, this::handleGetLdapUser);
		doDelete(LdapUser.class, this::handleDeleteLdapUser);
		doPut(LdapUser.class, this::handlePutLdapUser);
	}
	
	private CompletableFuture<Page<LdapUser>> handleListLdapUsers(final ListLdapUsers listLdapUsers) {
		log.debug("handleListLdapUsers");
		final Page.Builder<LdapUser> builder = new Page.Builder<>();
		
		doGetRequest(ldapApiAdminUrlBaseUsers)
			.ifPresent(response -> {
				try {
					JsonArray jsonArray = new JsonParser().parse(response).getAsJsonArray();
					Stream<JsonObject> stream = 
							sortJsonArray(jsonArray, "mail")
								.stream()
								.filter(o -> !ldapApiAdminMail.equals(o.get("mail").getAsString().trim()));
					
					if(!listLdapUsers.getAll()) {
						stream = stream
							.skip((listLdapUsers.getPage() - 1) * DEFAULT_ITEMS_PER_PAGE)
							.limit(DEFAULT_ITEMS_PER_PAGE);
					}
					
					List<JsonObject> list = stream.collect(Collectors.toList());
					
					addPageInfo(builder, listLdapUsers.getPage(), jsonArray.size() - 1);
					
					for(JsonObject object : list) {
						builder.add(new LdapUser(
							null,
							object.get("mail").getAsString(),
							object.get("fullName").getAsString(),
							object.get("lastName").getAsString(),
							null
						));
					}
				} catch(JsonParseException jpe) {
					log.debug("LDAP get user list request: can't parse json of response");
				} catch(IllegalStateException ise) {
					log.debug("LDAP get user list request: unexpected json response");
				} catch(Exception e) {
					log.debug("LDAP get user list request: something went wrong");
				}
			});
		
		return CompletableFuture.supplyAsync(() -> builder.build());
	}
	
	private CompletableFuture<Optional<LdapUser>> handleGetLdapUser(String email) {
		log.debug("handleGetLdapUser: {}", email);
		return CompletableFuture.supplyAsync(() -> getLdapUser(email));
	}
	
	private CompletableFuture<Response<?>> handlePutLdapUser(LdapUser user) {
		log.debug("handlePutLdapUser: {}", user.mail());
		boolean emailExists = getLdapUser(user.mail()).isPresent();
		CrudOperation operation = emailExists ? CrudOperation.UPDATE : CrudOperation.CREATE;
		
		if(ldapApiAdminMail.equals(user.mail().trim())) {
			return CompletableFuture.supplyAsync(() -> {
				return new Response<String>(operation, CrudResponse.NOK, user.mail());
			});
		}
		
		try {
			doPostPutRequest(ldapApiAdminUrlBaseUsers, emailExists, new Gson().toJson(user));
		} catch(Exception e) {
			log.debug("LDAP post/put user request: something went wrong performing the request");
			
			return CompletableFuture.supplyAsync(() -> {
				return new Response<String>(operation, CrudResponse.NOK, user.mail());
			});
		}
		
		return CompletableFuture.supplyAsync(() -> {
			return new Response<String>(operation, CrudResponse.OK, user.mail());
		});
	}
	
	private CompletableFuture<Response<?>> handleDeleteLdapUser(String email) {
		log.debug("handleDeleteLdapUser: {}", email);
		if(ldapApiAdminMail.equals(email.trim())) {
			return CompletableFuture.supplyAsync(() -> {
				return new Response<String>(CrudOperation.DELETE, CrudResponse.NOK, email);
			});
		}
		
		try {
			doDeleteRequest(ldapApiAdminUrlBaseUsers + "/" + email);
		} catch(Exception e) {
			log.debug("LDAP delete user request: something went wrong performing the request");
			
			return CompletableFuture.supplyAsync(() -> {
				return new Response<String>(CrudOperation.DELETE, CrudResponse.NOK, email);
			});
		}
		
		return CompletableFuture.supplyAsync(() -> {
			return new Response<String>(CrudOperation.DELETE, CrudResponse.OK, email);
		});
	}
	
	private Optional<LdapUser> getLdapUser(String email) {
		log.debug("getLdapUser: {}", email);
		if(ldapApiAdminMail.equals(email.trim())) return Optional.empty();
		
		return
			doGetRequest(ldapApiAdminUrlBaseUsers + "/" + email)
				.flatMap(response -> {
					try {
						JsonObject object = new JsonParser().parse(response).getAsJsonObject();
						
						return Optional.of(new LdapUser(
							null,
							object.get("mail").getAsString(),
							object.get("fullName").getAsString(),
							object.get("lastName").getAsString(),
							null
						));
					} catch(JsonParseException jpe) {
						log.debug("LDAP get user request: can't parse json of response");
					} catch(IllegalStateException ise) {
						log.debug("LDAP get user request: unexpected json response");
					} catch(Exception e) {
						log.debug("LDAP get user request: something went wrong");
					}
					
					return Optional.empty();
				});
	}
}

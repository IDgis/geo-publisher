package nl.idgis.publisher.admin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import nl.idgis.publisher.domain.query.ListLdapUsers;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.LdapUser;

public class LdapAdmin extends AbstractAdmin {
	
	private final String ldapApiAdminMail, ldapApiAdminPassword, ldapApiAdminUrlBaseUsers;
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	private final CloseableHttpClient httpClient = 
			HttpClientBuilder
				.create()
				.setRedirectStrategy(new LaxRedirectStrategy()).build();
	
	public LdapAdmin(ActorRef database, String ldapApiAdminMail, String ldapApiAdminPassword, String ldapApiAdminUrlBaseUsers) {
		super(database);
		
		this.ldapApiAdminMail = ldapApiAdminMail;
		this.ldapApiAdminPassword = ldapApiAdminPassword;
		this.ldapApiAdminUrlBaseUsers = ldapApiAdminUrlBaseUsers;
	}
	
	public static Props props(ActorRef database, String ldapApiAdminMail, String ldapApiAdminPassword, String ldapApiAdminUrlBaseUsers) {
		return Props.create(LdapAdmin.class, database, ldapApiAdminMail, ldapApiAdminPassword, ldapApiAdminUrlBaseUsers);
	}

	@Override
	protected void preStartAdmin() {
		doQuery (ListLdapUsers.class, this::handleListLdapUsers);
		doGet(LdapUser.class, this::handleGetLdapUser);
		doDelete(LdapUser.class, this::handleDeleteLdapUser);
		doPut(LdapUser.class, this::handlePutLdapUser);
		
	}
	
	private CompletableFuture<Page<LdapUser>> handleListLdapUsers (final ListLdapUsers listLdapUsers) {
		log.debug("handleListLdapUsers");
		final Page.Builder<LdapUser> builder = new Page.Builder<>();
		
		doGetRequest(ldapApiAdminUrlBaseUsers)
			.ifPresent(response -> {
				try {
					JsonArray jsonArray = new JsonParser().parse(response).getAsJsonArray();
					List<JsonObject> list = 
							sortJsonArray(jsonArray, "mail")
								.stream()
								.filter(o -> !ldapApiAdminMail.equals(o.get("mail").getAsString().trim()))
								.skip((listLdapUsers.getPage() - 1) * DEFAULT_ITEMS_PER_PAGE)
								.limit(DEFAULT_ITEMS_PER_PAGE)
								.collect(Collectors.toList());
					
					addPageInfo (builder, listLdapUsers.getPage(), jsonArray.size() - 1);
					
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
	
	private CompletableFuture<Optional<LdapUser>> handleGetLdapUser (String email) {
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
			log.debug("LDAP post/put request: something went wrong performing the request");
			
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
			log.debug("LDAP delete request: something went wrong performing the request");
			
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
	
	protected List<JsonObject> sortJsonArray(JsonArray array, String field) {
		List<JsonObject> objects = new ArrayList<>();
		array.forEach(item -> objects.add(item.getAsJsonObject()));
		
		Collections.sort(objects, new Comparator<JsonObject>() {
			@Override
			public int compare(JsonObject a, JsonObject b) {
				String first = new String();
				String second = new String();
				
				first = (String) a.get(field).getAsString().toLowerCase();
				second = (String) b.get(field).getAsString().toLowerCase();
				
				return first.compareTo(second);
			}
		});
		
		return objects;
	}
	
	private String getAuthorizationHeader() {
		String auth = ldapApiAdminMail + ":" + ldapApiAdminPassword;
		byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
		return "Basic " + new String(encodedAuth);
	}
	
	private Optional<String> doGetRequest(String url) {
		log.debug("doGetRequest: {}", url);
		HttpGet request = new HttpGet(url);
		request.addHeader("Authorization", getAuthorizationHeader());
		
		try (CloseableHttpResponse response = httpClient.execute(request)) {
			HttpEntity entity = response.getEntity();
			
			if (entity != null) {
				BufferedReader inputStream = new BufferedReader(new InputStreamReader(entity.getContent()));
				String inputLine;
				StringBuffer content = new StringBuffer();
				while ((inputLine = inputStream.readLine()) != null) content.append(inputLine);
				return Optional.of(content.toString());
			}
		} catch(IOException ioe) {
			log.debug("LDAP get request: something went wrong reading the input");
		} catch(UnsupportedOperationException uoe) {
			log.debug("LDAP get request: operation is not supported");
		}
		
		return Optional.empty();
	}
	
	private void doPostPutRequest(String url, boolean emailExists, String body) throws IOException {
		log.debug("doPostPutRequest: {}", url);
		HttpEntityEnclosingRequestBase request;
		if(emailExists) {
			request = new HttpPut(url);
		} else {
			request = new HttpPost(url);
		}
		
		request.addHeader("Authorization", getAuthorizationHeader());
		request.addHeader("Content-Type", "application/json");
		request.setEntity(new ByteArrayEntity(body.getBytes("UTF-8")));
		
		try (CloseableHttpResponse response = httpClient.execute(request)) {
			// do nothing
		}
	}
	
	private void doDeleteRequest(String url) throws IOException {
		log.debug("doDeleteRequest: {}", url);
		HttpDelete request = new HttpDelete(url);
		request.addHeader("Authorization", getAuthorizationHeader());
		
		try (CloseableHttpResponse response = httpClient.execute(request)) {
			// do nothing
		}
	}
}

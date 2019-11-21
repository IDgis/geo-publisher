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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import akka.actor.ActorRef;

public abstract class AbstractLdapAdmin extends AbstractAdmin {
	
	protected final String ldapApiAdminMail, ldapApiAdminPassword, ldapApiAdminUrlBaseUsers, ldapApiAdminUrlBaseOrganizations;
	private final CloseableHttpClient httpClient = 
			HttpClientBuilder
				.create()
				.setRedirectStrategy(new LaxRedirectStrategy()).build();
	
	public AbstractLdapAdmin(ActorRef database, String ldapApiAdminMail, String ldapApiAdminPassword, String ldapApiAdminUrlBaseUsers, String ldapApiAdminUrlBaseOrganizations) {
		super(database);

		this.ldapApiAdminMail = ldapApiAdminMail;
		this.ldapApiAdminPassword = ldapApiAdminPassword;
		this.ldapApiAdminUrlBaseUsers = ldapApiAdminUrlBaseUsers;
		this.ldapApiAdminUrlBaseOrganizations = ldapApiAdminUrlBaseOrganizations;
	}
	
	protected List<JsonObject> sortJsonArray(JsonArray array, String field) {
		List<JsonObject> objects = new ArrayList<>();
		array.forEach(item -> objects.add(item.getAsJsonObject()));
		
		Collections.sort(objects, new Comparator<JsonObject>() {
			@Override
			public int compare(JsonObject a, JsonObject b) {
				String first = new String();
				String second = new String();
				
				first = a.get(field).getAsString().toLowerCase();
				second = b.get(field).getAsString().toLowerCase();
				
				return first.compareTo(second);
			}
		});
		
		return objects;
	}
	
	protected List<String> sortStringList(List<String> list) {
		Collections.sort(list, new Comparator<String>() {
			@Override
			public int compare(String a, String b) {
				return a.toLowerCase().compareTo(b.toLowerCase());
			}
		});
		
		return list;
	}
	
	protected String getAuthorizationHeader() {
		String auth = ldapApiAdminMail + ":" + ldapApiAdminPassword;
		byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
		return "Basic " + new String(encodedAuth);
	}
	
	protected Optional<String> doGetRequest(String url) {
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
	
	protected void doPostPutRequest(String url, boolean emailExists, String body) throws IOException {
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
	
	protected void doDeleteRequest(String url) throws IOException {
		log.debug("doDeleteRequest: {}", url);
		HttpDelete request = new HttpDelete(url);
		request.addHeader("Authorization", getAuthorizationHeader());
		
		try (CloseableHttpResponse response = httpClient.execute(request)) {
			// do nothing
		}
	}
}

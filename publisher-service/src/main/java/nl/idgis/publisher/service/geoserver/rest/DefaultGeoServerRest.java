package nl.idgis.publisher.service.geoserver.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.HttpURLConnection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

public class DefaultGeoServerRest implements GeoServerRest {
	
	private final String authorization;
	
	private final String serviceLocation;
	
	private final DocumentBuilder documentBuilder;
	
	private final XPath xpath;
	
	private final XMLOutputFactory outputFactory;
	
	private final AsyncHttpClient asyncHttpClient;
	
	public DefaultGeoServerRest(String serviceLocation, String user, String password) throws Exception {		
		this.serviceLocation = serviceLocation;
		this.authorization = "Basic " + new String(Base64.encodeBase64((user + ":" + password).getBytes()));		
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		documentBuilder = dbf.newDocumentBuilder();
		
		xpath = XPathFactory.newInstance().newXPath();
		
		outputFactory = XMLOutputFactory.newInstance();
		
		asyncHttpClient = new AsyncHttpClient();
	}	
	
	private CompletableFuture<Document> getDocument(String path) {
		CompletableFuture<Document> future = new CompletableFuture<>();
		
		asyncHttpClient.prepareGet(path + ".xml")
			.addHeader("Authorization", authorization)
			.execute(new AsyncCompletionHandler<Response>() {

			@Override
			public Response onCompleted(Response response) throws Exception {
				try {
					InputStream stream = response.getResponseBodyAsStream();
					Document document = documentBuilder.parse(stream);
					stream.close();
					
					future.complete(document);
				} catch(Exception e) {
					future.completeExceptionally(e);
				}
				
				return response;
			}
			
			@Override
		    public void onThrowable(Throwable t){
				future.completeExceptionally(t);
		    }
			
		});
		
		return future;
	}	
	
	private CompletableFuture<Integer> sendDocument(String path, byte[] document) {
		CompletableFuture<Integer> future = new CompletableFuture<>();
		
		asyncHttpClient.preparePost(path)
			.addHeader("Authorization", authorization)
			.addHeader("Content-type", "text/xml")
			.setBody(document)
			.execute(new AsyncCompletionHandler<Response>() {

				@Override
				public Response onCompleted(Response response) throws Exception {
					future.complete(response.getStatusCode());

					return response;
				}
				
				@Override
			    public void onThrowable(Throwable t){
					future.completeExceptionally(t);
			    }
			});
		
		return future;
	}
	
	@Override
	public CompletableFuture<Boolean> addWorkspace(Workspace workspace) {
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			
			XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(outputStream);
			streamWriter.writeStartDocument();
				streamWriter.writeStartElement("workspace");
					streamWriter.writeStartElement("name");
						streamWriter.writeCharacters(workspace.getName());
					streamWriter.writeEndElement();
				streamWriter.writeEndElement();
			streamWriter.writeEndDocument();
			streamWriter.close();
			
			outputStream.close();
			
			sendDocument(getWorkspacesPath(), outputStream.toByteArray()).whenComplete((responseCode, t) -> {
				if(t != null) {
					future.completeExceptionally(t);
				} else {
					future.complete(responseCode == HttpURLConnection.HTTP_CREATED);
				}
			});
		} catch(Exception e) {
			future.completeExceptionally(e);
		}
		
		return future;
	}

	private String getWorkspacesPath() {
		return serviceLocation + "workspaces";
	}	
	
	@Override
	public CompletableFuture<List<Workspace>> getWorkspaces() {
		CompletableFuture<List<Workspace>> future = new CompletableFuture<>();

		getDocument(getWorkspacesPath()).whenComplete((document, t) -> {
			if(t != null) {
				future.completeExceptionally(t);
			} else {			
				try {
					List<Workspace> retval = new ArrayList<>();
				
					NodeList result = (NodeList)xpath.evaluate("/workspaces/workspace/name/text()", document, XPathConstants.NODESET);
					for(int i = 0; i < result.getLength(); i++) {
						Node n = result.item(i);
						retval.add(new Workspace(n.getTextContent()));
					}
					
					future.complete(retval);
				} catch(Exception e) {
					future.completeExceptionally(e);
				}
			}
		});
		
		return future;
	}
	
	private CompletableFuture<DataStore> getDataStore(Workspace workspace, String dataStoreName) {
		CompletableFuture<DataStore> future = new CompletableFuture<>();
		
		getDocument(getDataStorePath(workspace, dataStoreName)).whenComplete((document, t) -> {
			if(t != null) {
				future.completeExceptionally(t);
			} else {
				try {
					String name = (String)xpath.evaluate("/dataStore/name/text()", document, XPathConstants.STRING);
					
					Map<String, String> connectionParameters = new HashMap<>();
					NodeList result = (NodeList)xpath.evaluate("/dataStore/connectionParameters/entry", document, XPathConstants.NODESET);
					for(int i = 0; i < result.getLength(); i++) {
						Node n = result.item(i);
						
						String key = n.getAttributes().getNamedItem("key").getNodeValue();
						String value = n.getTextContent();
						connectionParameters.put(key, value);			
					}
					
					future.complete(new DataStore(name, connectionParameters));
				} catch(Exception e) {
					future.completeExceptionally(e);
				}
			}
		});
		
		return future;
	}

	private String getDataStorePath(Workspace workspace, String dataStoreName) {
		return getDataStoresPath(workspace) + "/" + dataStoreName;
	}

	@Override
	public CompletableFuture<List<CompletableFuture<DataStore>>> getDataStores(Workspace workspace) {
		CompletableFuture<List<CompletableFuture<DataStore>>> future = new CompletableFuture<>();
		
		getDocument(getDataStoresPath(workspace)).whenComplete((document, t) -> {
			if(t != null) {
				future.completeExceptionally(t);
			} else {
				try {
					List<CompletableFuture<DataStore>> retval = new ArrayList<>();
					
					NodeList result = (NodeList)xpath.evaluate("/dataStores/dataStore/name/text()", document, XPathConstants.NODESET);
					for(int i = 0; i < result.getLength(); i++) {
						Node n = result.item(i);
						retval.add(getDataStore(workspace, n.getTextContent()));
					}
					
					future.complete(retval);
				} catch(Exception e) {
					future.completeExceptionally(e);
				}
			}
		});
		
		return future;
	}

	@Override
	public CompletableFuture<Boolean> addDataStore(Workspace workspace, DataStore dataStore) {
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			
			XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(outputStream);
			streamWriter.writeStartDocument();
			streamWriter.writeStartElement("dataStore");
				streamWriter.writeStartElement("name");
					streamWriter.writeCharacters(dataStore.getName());
				streamWriter.writeEndElement();
				streamWriter.writeStartElement("connectionParameters");
					for(Map.Entry<String, String> connectionParameter : dataStore.getConnectionParameters().entrySet()) {
						streamWriter.writeStartElement(connectionParameter.getKey());
						streamWriter.writeCharacters(connectionParameter.getValue());
						streamWriter.writeEndElement();
					}
				streamWriter.writeEndElement();
			streamWriter.writeEndDocument();
			streamWriter.close();
			
			outputStream.close();
			
			sendDocument(getDataStoresPath(workspace), outputStream.toByteArray()).whenComplete((responseCode, t) -> {
				if(t != null) {
					future.completeExceptionally(t);
				} else {
					future.complete(responseCode == HttpURLConnection.HTTP_CREATED);
				}
			});				
		} catch(Exception e) {
			future.completeExceptionally(e);
		}
		
		return future;
	}

	private String getDataStoresPath(Workspace workspace) {
		return getWorkspacesPath() + "/" + workspace.getName() + "/datastores";
	}
	
	private String getFeatureTypePath(Workspace workspace, DataStore dataStore, String featureTypeName) {
		return getFeatureTypesPath(workspace, dataStore) + "/" + featureTypeName;
	}
	
	private CompletableFuture<FeatureType> getFeatureType(Workspace workspace, DataStore dataStore, String featureTypeName) {
		CompletableFuture<FeatureType> future = new CompletableFuture<>();
		
		getDocument(getFeatureTypePath(workspace, dataStore, featureTypeName)).whenComplete((document, t) -> {
			if(t != null) {
				future.completeExceptionally(t);
			} else {
				try {
					String name = (String)xpath.evaluate("/featureType/name/text()", document, XPathConstants.STRING);
					String nativeName = (String)xpath.evaluate("/featureType/nativeName/text()", document, XPathConstants.STRING);
					
					List<Attribute> attributes = new ArrayList<>();
					NodeList result = (NodeList)xpath.evaluate("/featureType/attributes/attribute", document, XPathConstants.NODESET);
					for(int i = 0; i < result.getLength(); i++) {
						Node n = result.item(i);
						
						String attributeName = xpath.evaluate("name/text()", n);
						attributes.add(new Attribute(attributeName));
					}
					
					future.complete(new FeatureType(name, nativeName, Collections.unmodifiableList(attributes)));					
				} catch(Exception e) {
					future.completeExceptionally(e);
				}
			}
		});
		
		return future;
	}
	
	@Override
	public CompletableFuture<List<CompletableFuture<FeatureType>>> getFeatureTypes(Workspace workspace, DataStore dataStore) {
		CompletableFuture<List<CompletableFuture<FeatureType>>> future = new CompletableFuture<>();
		
		getDocument(getFeatureTypesPath(workspace, dataStore)).whenComplete((document, t) -> {
			if(t != null) {
				future.completeExceptionally(t);
			} else {
				try {
					List<CompletableFuture<FeatureType>> retval = new ArrayList<>();
					
					NodeList result = (NodeList)xpath.evaluate("/featureTypes/featureType/name/text()", document, XPathConstants.NODESET);
					for(int i = 0; i < result.getLength(); i++) {
						Node n = result.item(i);
						retval.add(getFeatureType(workspace, dataStore, n.getTextContent()));
					}
					
					future.complete(retval);
				} catch(Exception e) {
					future.completeExceptionally(e);
				}
			}
		});
		
		return future;
	}
	
	@Override
	public CompletableFuture<Boolean> addFeatureType(Workspace workspace, DataStore dataStore, FeatureType featureType) {
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			
			XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(outputStream);
			streamWriter.writeStartDocument();
			streamWriter.writeStartElement("featureType");
				streamWriter.writeStartElement("name");
					streamWriter.writeCharacters(featureType.getName());
				streamWriter.writeEndElement();
				
				String nativeName = featureType.getNativeName();
				if(nativeName != null) {
					streamWriter.writeStartElement("nativeName");
						streamWriter.writeCharacters(nativeName);
					streamWriter.writeEndElement();
				}
			streamWriter.writeEndElement();
			streamWriter.writeEndDocument();
			streamWriter.close();
			
			outputStream.close();
			
			sendDocument(getFeatureTypesPath(workspace, dataStore), outputStream.toByteArray()).whenComplete((responseCode, t) -> {
				if(t != null) {
					future.completeExceptionally(t);
				} else {
					future.complete(responseCode == HttpURLConnection.HTTP_CREATED);
				}
			});
		} catch(Exception e) {
			future.completeExceptionally(e);
		}
		
		return future;
	}
	
	private String getFeatureTypesPath(Workspace workspace, DataStore dataStore) {
		return getDataStorePath(workspace, dataStore.getName()) + "/featuretypes";
	}

	@Override
	public void close() throws IOException {
		asyncHttpClient.close();
	}
}

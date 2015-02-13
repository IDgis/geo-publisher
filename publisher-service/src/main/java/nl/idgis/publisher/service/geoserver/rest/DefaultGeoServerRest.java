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
	
	private static final String RECURSE = "?recurse=true";

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
	
	private CompletableFuture<Document> get(String path) {
		CompletableFuture<Document> future = new CompletableFuture<>();
		
		asyncHttpClient.prepareGet(path + ".xml")
			.addHeader("Authorization", authorization)
			.execute(new AsyncCompletionHandler<Response>() {

			@Override
			public Response onCompleted(Response response) throws Exception {
				try {
					int responseCode = response.getStatusCode();
					if(responseCode == HttpURLConnection.HTTP_OK) {					
						InputStream stream = response.getResponseBodyAsStream();
						Document document = documentBuilder.parse(stream);
					
						stream.close();
						
						future.complete(document);
					} else {
						future.completeExceptionally(new GeoServerException(responseCode));
					}
				} catch(Exception e) {
					future.completeExceptionally(new GeoServerException(e));
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
	
	private CompletableFuture<Void> delete(String path) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		
		asyncHttpClient.prepareDelete(path)
			.addHeader("Authorization", authorization)
			.execute(new AsyncCompletionHandler<Response>() {

				@Override
				public Response onCompleted(Response response) throws Exception {
					int responseCode = response.getStatusCode();					
					if(responseCode != HttpURLConnection.HTTP_OK) {	
						future.completeExceptionally(new GeoServerException(responseCode));
					} else {
						future.complete(null);
					}
					
					return response;
				}
				
				@Override
			    public void onThrowable(Throwable t){
					future.completeExceptionally(new GeoServerException(t));
			    }
				
			});
		
		return future;
	}
	
	private CompletableFuture<Void> post(String path, byte[] document) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		
		asyncHttpClient.preparePost(path)
			.addHeader("Authorization", authorization)
			.addHeader("Content-type", "text/xml")
			.setBody(document)
			.execute(new AsyncCompletionHandler<Response>() {

				@Override
				public Response onCompleted(Response response) throws Exception {
					int responseCode = response.getStatusCode();
					if(responseCode != HttpURLConnection.HTTP_CREATED) {	
						future.completeExceptionally(new GeoServerException(responseCode));
					} else {
						future.complete(null);
					}

					return response;
				}
				
				@Override
			    public void onThrowable(Throwable t){
					future.completeExceptionally(new GeoServerException(t));
			    }
			});
		
		return future;
	}
	
	@Override
	public CompletableFuture<Void> postWorkspace(Workspace workspace) {
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
			
			return post(getWorkspacesPath(), outputStream.toByteArray());
		} catch(Exception e) {
			return failure(e);
		}
	}

	private <T> CompletableFuture<T> failure(Exception e) {
		CompletableFuture<T> future = new CompletableFuture<>();
		future.completeExceptionally(new GeoServerException(e));
		return future;
	}
	
	private String getWorkspacePath(Workspace workspace) {
		return getWorkspacesPath() + "/" + workspace.getName();
	}

	private String getWorkspacesPath() {
		return serviceLocation + "workspaces";
	}	
	
	@Override
	public CompletableFuture<List<Workspace>> getWorkspaces() {
		CompletableFuture<List<Workspace>> future = new CompletableFuture<>();

		get(getWorkspacesPath()).whenComplete((document, t) -> {
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
		
		get(getDataStorePath(workspace, dataStoreName)).whenComplete((document, t) -> {
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
		
		get(getDataStoresPath(workspace)).whenComplete((document, t) -> {
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
	public CompletableFuture<Void> postDataStore(Workspace workspace, DataStore dataStore) {
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
			
			return post(getDataStoresPath(workspace), outputStream.toByteArray());			
		} catch(Exception e) {
			return failure(e);
		}
	}

	private String getDataStoresPath(Workspace workspace) {
		return getWorkspacesPath() + "/" + workspace.getName() + "/datastores";
	}
	
	private String getFeatureTypePath(Workspace workspace, DataStore dataStore, String featureTypeName) {
		return getFeatureTypesPath(workspace, dataStore) + "/" + featureTypeName;
	}
	
	private CompletableFuture<FeatureType> getFeatureType(Workspace workspace, DataStore dataStore, String featureTypeName) {
		CompletableFuture<FeatureType> future = new CompletableFuture<>();
		
		get(getFeatureTypePath(workspace, dataStore, featureTypeName)).whenComplete((document, t) -> {
			if(t != null) {
				future.completeExceptionally(t);
			} else {
				try {
					String name = (String)xpath.evaluate("/featureType/name/text()", document, XPathConstants.STRING);
					String nativeName = (String)xpath.evaluate("/featureType/nativeName/text()", document, XPathConstants.STRING);
					String title = (String)xpath.evaluate("/featureType/title/text()", document, XPathConstants.STRING);
					String abstr = (String)xpath.evaluate("/featureType/abstract/text()", document, XPathConstants.STRING);
					
					List<Attribute> attributes = new ArrayList<>();
					NodeList result = (NodeList)xpath.evaluate("/featureType/attributes/attribute", document, XPathConstants.NODESET);
					for(int i = 0; i < result.getLength(); i++) {
						Node n = result.item(i);
						
						String attributeName = xpath.evaluate("name/text()", n);
						attributes.add(new Attribute(attributeName));
					}
					
					future.complete(new FeatureType(name, nativeName, title, abstr, Collections.unmodifiableList(attributes)));					
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
		
		get(getFeatureTypesPath(workspace, dataStore)).whenComplete((document, t) -> {
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
	public CompletableFuture<Void> postFeatureType(Workspace workspace, DataStore dataStore, FeatureType featureType) {
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			
			XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(outputStream);
			streamWriter.writeStartDocument();
			streamWriter.writeStartElement("featureType");
				streamWriter.writeStartElement("name");
					streamWriter.writeCharacters(featureType.getName());
				streamWriter.writeEndElement();
				
				String nativeName = featureType.getNativeName();				
				streamWriter.writeStartElement("nativeName");
					streamWriter.writeCharacters(nativeName);
				streamWriter.writeEndElement();				
				
				String title = featureType.getTitle();
				if(title != null) {
					streamWriter.writeStartElement("title");
						streamWriter.writeCharacters(title);
					streamWriter.writeEndElement();
				}
				
				String abstr = featureType.getAbstract();
				if(abstr != null) {
					streamWriter.writeStartElement("abstract");
						streamWriter.writeCharacters(abstr);
					streamWriter.writeEndElement();
				}
			streamWriter.writeEndElement();
			streamWriter.writeEndDocument();
			streamWriter.close();
			
			outputStream.close();
			
			return post(getFeatureTypesPath(workspace, dataStore), outputStream.toByteArray());
		} catch(Exception e) {			
			return failure(e);
		}
	}
	
	private String getFeatureTypesPath(Workspace workspace, DataStore dataStore) {
		return getDataStorePath(workspace, dataStore.getName()) + "/featuretypes";
	}

	@Override
	public void close() throws IOException {
		asyncHttpClient.close();
	}
	
	private String getLayerGroupPath(Workspace workspace, String layerGroupName) {
		return getLayerGroupsPath(workspace) + "/" + layerGroupName;
	}
	
	private CompletableFuture<LayerGroup> getLayerGroup(Workspace workspace, String layerGroupName) {
		CompletableFuture<LayerGroup> future = new CompletableFuture<>();
		
		get(getLayerGroupPath(workspace, layerGroupName)).whenComplete((document, t) -> {
			if(t != null) {
				future.completeExceptionally(t);
			} else {
				try {
					List<String> layers = new ArrayList<>();
					
					NodeList result = (NodeList)xpath.evaluate("/layerGroup/publishables/published[@type='layer']/name/text()", document, XPathConstants.NODESET);
					for(int i = 0; i < result.getLength(); i++) {
						Node n = result.item(i);
						layers.add(n.getTextContent());
					}
					
					String title = (String)xpath.evaluate("/layerGroup/title/text()", document, XPathConstants.STRING);
					String abstr = (String)xpath.evaluate("/layerGroup/abstractTxt/text()", document, XPathConstants.STRING);
					
					future.complete(new LayerGroup(layerGroupName, title, abstr, Collections.unmodifiableList(layers)));
				} catch(Exception e) {
					future.completeExceptionally(e);
				}
			}
		});
		
		return future;
	}
	
	private String getLayerGroupsPath(Workspace workspace) {
		return getWorkspacesPath() + "/" + workspace.getName() + "/layergroups";
	}

	@Override
	public CompletableFuture<List<CompletableFuture<LayerGroup>>> getLayerGroups(Workspace workspace) {
		CompletableFuture<List<CompletableFuture<LayerGroup>>> future = new CompletableFuture<>();
		
		get(getLayerGroupsPath(workspace)).whenComplete((document, t) -> {
			if(t != null) {
				future.completeExceptionally(t);
			} else {
				try {
					List<CompletableFuture<LayerGroup>> retval = new ArrayList<>();
					
					NodeList result = (NodeList)xpath.evaluate("/layerGroups/layerGroup/name/text()", document, XPathConstants.NODESET);
					for(int i = 0; i < result.getLength(); i++) {
						Node n = result.item(i);
						retval.add(getLayerGroup(workspace, n.getTextContent()));
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
	public CompletableFuture<Void> postLayerGroup(Workspace workspace, LayerGroup layerGroup) {
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			
			XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(outputStream);
			streamWriter.writeStartDocument();
			streamWriter.writeStartElement("layerGroup");
				streamWriter.writeStartElement("name");
					streamWriter.writeCharacters(layerGroup.getName());
				streamWriter.writeEndElement();
				
				streamWriter.writeStartElement("mode");
					streamWriter.writeCharacters("NAMED");
				streamWriter.writeEndElement();
				
				String title = layerGroup.getTitle();
				if(title != null) {
					streamWriter.writeStartElement("title");
						streamWriter.writeCharacters(title);
					streamWriter.writeEndElement();
				}
				
				String abstr = layerGroup.getAbstract();
				if(abstr != null) {
					streamWriter.writeStartElement("abstractTxt");
						streamWriter.writeCharacters(abstr);
					streamWriter.writeEndElement();
				}
				
				streamWriter.writeStartElement("layers");
				for(String layer : layerGroup.getLayers()) {
					streamWriter.writeStartElement("layer");
						streamWriter.writeCharacters(layer);
					streamWriter.writeEndElement();
				}
				streamWriter.writeEndElement();
			streamWriter.writeEndElement();
			streamWriter.writeEndDocument();
			streamWriter.close();
			
			outputStream.close();
			
			return post(getLayerGroupsPath(workspace), outputStream.toByteArray());
		} catch(Exception e) {
			return failure(e);
		}
	}

	@Override
	public CompletableFuture<Void> deleteDataStore(Workspace workspace, DataStore dataStore) {		
		return delete(getDataStorePath(workspace, dataStore.getName()) + RECURSE);
	}

	@Override
	public CompletableFuture<Void> deleteFeatureType(Workspace workspace, DataStore dataStore, FeatureType featureType) {
		return delete(getFeatureTypePath(workspace, dataStore, featureType.getName()) + RECURSE);
	}

	@Override
	public CompletableFuture<Void> deleteLayerGroup(Workspace workspace, LayerGroup layerGroup) {
		return delete(getLayerGroupPath(workspace, layerGroup.getName()));
	}

	@Override
	public CompletableFuture<Void> deleteWorkspace(Workspace workspace) { 
		return delete(getWorkspacePath(workspace) + RECURSE);
	}
}

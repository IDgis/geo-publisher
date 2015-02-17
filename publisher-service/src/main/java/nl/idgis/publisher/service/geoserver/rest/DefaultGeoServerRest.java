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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
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
	
	private final AsyncHttpClient asyncHttpClient;
	
	public DefaultGeoServerRest(String serviceLocation, String user, String password) throws Exception {		
		this.serviceLocation = serviceLocation;
		this.authorization = "Basic " + new String(Base64.encodeBase64((user + ":" + password).getBytes()));
		
		asyncHttpClient = new AsyncHttpClient();
	}
	
	private CompletableFuture<Optional<Document>> get(String path) {
		CompletableFuture<Optional<Document>> future = new CompletableFuture<>();
		
		asyncHttpClient.prepareGet(path + ".xml")
			.addHeader("Authorization", authorization)
			.execute(new AsyncCompletionHandler<Response>() {

			@Override
			public Response onCompleted(Response response) throws Exception {
				try {
					int responseCode = response.getStatusCode();
					if(responseCode == HttpURLConnection.HTTP_OK) {
						InputStream stream = response.getResponseBodyAsStream();
						
						DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
						DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
						
						Document document = documentBuilder.parse(stream);
						
						stream.close();
						
						future.complete(Optional.of(document));
					} else if(responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
						future.complete(Optional.empty());
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
	
	private CompletableFuture<Void> put(String path, byte[] document) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		
		asyncHttpClient.preparePut(path)
			.addHeader("Authorization", authorization)
			.addHeader("Content-type", "text/xml")
			.setBody(document)
			.execute(new AsyncCompletionHandler<Response>() {

				@Override
				public Response onCompleted(Response response) throws Exception {
					System.out.println("response: " + response.getResponseBody());
					
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
	
	private String getServiceSettingsPath(Workspace workspace, ServiceType serviceType) {
		return serviceLocation + "services/" + serviceType.name().toLowerCase() + "/workspaces/" 
			+ workspace.getName() + "/settings";
	}
	
	@Override
	public CompletableFuture<Void> putServiceSettings(Workspace workspace, ServiceType serviceType, ServiceSettings serviceSettings) {
		try {
			String serviceTypeName = serviceType.name().toLowerCase();
			
			// we have to use the default settings as a template otherwise all kind of 
			// essential settings are set to null, causing NPE when using the service.
			InputStream defaultServiceSettings =
				getClass().getResourceAsStream(
					"default-" + serviceTypeName + "-settings.xml");
			
			Objects.requireNonNull(defaultServiceSettings);
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document document = db.parse(defaultServiceSettings);
			
			XPath xpath = XPathFactory.newInstance().newXPath();
					
			String title = serviceSettings.getTitle();
			if(title != null) {
				Node titleNode = (Node)xpath.evaluate(serviceTypeName + "/title/text()", document, XPathConstants.NODE);
				titleNode.setTextContent(title);
			}
			
			String abstr = serviceSettings.getAbstract();
			if(abstr != null) {
				Node abstractNode = (Node)xpath.evaluate(serviceTypeName + "/abstrct/text()", document, XPathConstants.NODE);
				abstractNode.setTextContent(abstr);
			}
			
			List<String> keywords = serviceSettings.getKeywords();
			if(keywords != null) {
				Node keywordsNode = (Node)xpath.evaluate(serviceTypeName + "/keywords", document, XPathConstants.NODE);				
				
				while(keywordsNode.hasChildNodes()) {
					keywordsNode.removeChild(keywordsNode.getFirstChild());
				}
				
				for(String keyword : keywords) {
					Node keywordNode = document.createElement("keyword");
					keywordNode.appendChild(document.createTextNode(keyword));					
					keywordsNode.appendChild(keywordNode);
				}
			}
			
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer t = tf.newTransformer();
			
			t.transform(new DOMSource(document), new StreamResult(os));
			
			return put(getServiceSettingsPath(workspace, serviceType), os.toByteArray());
		} catch(Exception e) {
			return failure(e);
		}
	}
	
	@Override
	public CompletableFuture<Void> postWorkspace(Workspace workspace) {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			
			XMLOutputFactory of = XMLOutputFactory.newInstance();
			XMLStreamWriter sw = of.createXMLStreamWriter(os);
			sw.writeStartDocument();
				sw.writeStartElement("workspace");
					sw.writeStartElement("name");
						sw.writeCharacters(workspace.getName());
					sw.writeEndElement();
				sw.writeEndElement();
			sw.writeEndDocument();
			sw.close();
			
			os.close();
			
			return post(getWorkspacesPath(), os.toByteArray());
		} catch(Exception e) {
			return failure(e);
		}
	}

	private <T> CompletableFuture<T> failure(Exception e) {
		CompletableFuture<T> future = new CompletableFuture<>();
		future.completeExceptionally(new GeoServerException(e));
		return future;
	}
	
	private String getWorkspacePath(String workspaceId) {
		return getWorkspacesPath() + "/" + workspaceId;
	}
	
	private String getWorkspacePath(Workspace workspace) {
		return getWorkspacePath(workspace.getName());
	}

	private String getWorkspacesPath() {
		return serviceLocation + "workspaces";
	}	
	
	@Override
	public CompletableFuture<List<Workspace>> getWorkspaces() {
		CompletableFuture<List<Workspace>> future = new CompletableFuture<>();

		get(getWorkspacesPath()).whenComplete((optionalDocument, t) -> {
			if(t != null) {
				future.completeExceptionally(t);
			} else {			
				try {
					Document document = optionalDocument.get();
					List<Workspace> retval = new ArrayList<>();
				
					XPath xpath = XPathFactory.newInstance().newXPath();
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
	
	@Override
	public CompletableFuture<Optional<DataStore>> getDataStore(Workspace workspace, String dataStoreName) {
		CompletableFuture<Optional<DataStore>> future = new CompletableFuture<>();
		
		get(getDataStorePath(workspace, dataStoreName)).whenComplete((optionalDocument, t) -> {
			if(t != null) {
				future.completeExceptionally(t);
			} else {
				try {
					if(optionalDocument.isPresent()) {
						Document document = optionalDocument.get();
						
						XPath xpath = XPathFactory.newInstance().newXPath();
						String name = (String)xpath.evaluate("/dataStore/name/text()", document, XPathConstants.STRING);
						
						Map<String, String> connectionParameters = new HashMap<>();
						NodeList result = (NodeList)xpath.evaluate("/dataStore/connectionParameters/entry", document, XPathConstants.NODESET);
						for(int i = 0; i < result.getLength(); i++) {
							Node n = result.item(i);
							
							String key = n.getAttributes().getNamedItem("key").getNodeValue();
							String value = n.getTextContent();
							connectionParameters.put(key, value);			
						}
						
						future.complete(Optional.of(new DataStore(name, connectionParameters)));
					} else {
						future.complete(Optional.empty());
					}
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
	
	private <T> T present(Optional<T> optional) {
		return optional.get();
	}

	@Override
	public CompletableFuture<List<CompletableFuture<DataStore>>> getDataStores(Workspace workspace) {
		CompletableFuture<List<CompletableFuture<DataStore>>> future = new CompletableFuture<>();
		
		get(getDataStoresPath(workspace)).whenComplete((optionalDocument, t) -> {
			if(t != null) {
				future.completeExceptionally(t);
			} else {
				try {
					Document document = optionalDocument.get();
					List<CompletableFuture<DataStore>> retval = new ArrayList<>();
					
					XPath xpath = XPathFactory.newInstance().newXPath();
					NodeList result = (NodeList)xpath.evaluate("/dataStores/dataStore/name/text()", document, XPathConstants.NODESET);
					for(int i = 0; i < result.getLength(); i++) {
						Node n = result.item(i);
						retval.add(getDataStore(workspace, n.getTextContent()).thenApply(this::present));
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
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			
			XMLOutputFactory of = XMLOutputFactory.newInstance();
			XMLStreamWriter sw = of.createXMLStreamWriter(os);
			sw.writeStartDocument();
			sw.writeStartElement("dataStore");
				sw.writeStartElement("name");
					sw.writeCharacters(dataStore.getName());
				sw.writeEndElement();
				sw.writeStartElement("connectionParameters");
					for(Map.Entry<String, String> connectionParameter : dataStore.getConnectionParameters().entrySet()) {
						sw.writeStartElement(connectionParameter.getKey());
						sw.writeCharacters(connectionParameter.getValue());
						sw.writeEndElement();
					}
				sw.writeEndElement();
			sw.writeEndDocument();
			sw.close();
			
			os.close();
			
			return post(getDataStoresPath(workspace), os.toByteArray());			
		} catch(Exception e) {
			return failure(e);
		}
	}

	private String getDataStoresPath(Workspace workspace) {
		return getWorkspacesPath() + "/" + workspace.getName() + "/datastores";
	}
	
	private String getFeatureTypePath(Workspace workspace, DataStore dataStore, FeatureType featureType) {
		return getFeatureTypePath(workspace, dataStore, featureType.getName());
	}
	
	private String getFeatureTypePath(Workspace workspace, DataStore dataStore, String featureTypeName) {
		return getFeatureTypesPath(workspace, dataStore) + "/" + featureTypeName;
	}
	
	private CompletableFuture<Optional<FeatureType>> getFeatureType(Workspace workspace, DataStore dataStore, String featureTypeName) {
		CompletableFuture<Optional<FeatureType>> future = new CompletableFuture<>();
		
		get(getFeatureTypePath(workspace, dataStore, featureTypeName)).whenComplete((optionalDocument, t) -> {
			if(t != null) {
				future.completeExceptionally(t);
			} else {
				try {
					if(optionalDocument.isPresent()) {	
						Document document = optionalDocument.get();
						
						XPath xpath = XPathFactory.newInstance().newXPath();
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
						
						future.complete(Optional.of(new FeatureType(name, nativeName, title, abstr, Collections.unmodifiableList(attributes))));
					} else {
						future.complete(Optional.empty());
					}
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
		
		get(getFeatureTypesPath(workspace, dataStore)).whenComplete((optionalDocument, t) -> {
			if(t != null) {
				future.completeExceptionally(t);
			} else {
				try {
					Document document = optionalDocument.get();
					List<CompletableFuture<FeatureType>> retval = new ArrayList<>();
					
					XPath xpath = XPathFactory.newInstance().newXPath();
					NodeList result = (NodeList)xpath.evaluate("/featureTypes/featureType/name/text()", document, XPathConstants.NODESET);
					for(int i = 0; i < result.getLength(); i++) {
						Node n = result.item(i);
						retval.add(getFeatureType(workspace, dataStore, n.getTextContent()).thenApply(this::present));
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
	public CompletableFuture<Void> putFeatureType(Workspace workspace, DataStore dataStore, FeatureType featureType) {
		try {
			return put(getFeatureTypePath(workspace, dataStore, featureType), getFeatureTypeDocument(featureType));
		} catch(Exception e) {			
			return failure(e);
		}
	}

	private byte[] getFeatureTypeDocument(FeatureType featureType) throws XMLStreamException, IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		XMLOutputFactory of = XMLOutputFactory.newInstance();
		XMLStreamWriter sw = of.createXMLStreamWriter(os);
		sw.writeStartDocument();
		sw.writeStartElement("featureType");
			sw.writeStartElement("name");
				sw.writeCharacters(featureType.getName());
			sw.writeEndElement();
			
			String nativeName = featureType.getNativeName();				
			sw.writeStartElement("nativeName");
				sw.writeCharacters(nativeName);
			sw.writeEndElement();				
			
			String title = featureType.getTitle();
			if(title != null) {
				sw.writeStartElement("title");
					sw.writeCharacters(title);
				sw.writeEndElement();
			}
			
			String abstr = featureType.getAbstract();
			if(abstr != null) {
				sw.writeStartElement("abstract");
					sw.writeCharacters(abstr);
				sw.writeEndElement();
			}
			
			sw.writeStartElement("enabled");
				sw.writeCharacters("true");
			sw.writeEndElement();
			
		sw.writeEndElement();
		sw.writeEndDocument();
		sw.close();
		
		os.close();
		
		return os.toByteArray();		
	}
	
	@Override
	public CompletableFuture<Void> postFeatureType(Workspace workspace, DataStore dataStore, FeatureType featureType) {
		try {
			return post(getFeatureTypesPath(workspace, dataStore), getFeatureTypeDocument(featureType));
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
	
	private String getLayerGroupPath(Workspace workspace, LayerGroup layerGroup) { 
		return getLayerGroupPath(workspace, layerGroup.getName());
	}
	
	private String getLayerGroupPath(Workspace workspace, String layerGroupName) {
		return getLayerGroupsPath(workspace) + "/" + layerGroupName;
	}
	
	private CompletableFuture<Optional<LayerGroup>> getLayerGroup(Workspace workspace, String layerGroupName) {
		CompletableFuture<Optional<LayerGroup>> future = new CompletableFuture<>();
		
		get(getLayerGroupPath(workspace, layerGroupName)).whenComplete((optionalDocument, t) -> {
			if(t != null) {
				future.completeExceptionally(t);
			} else {
				try {
					if(optionalDocument.isPresent()) {
						Document document = optionalDocument.get();
						List<LayerRef> layers = new ArrayList<>();
						
						XPath xpath = XPathFactory.newInstance().newXPath();
						NodeList result = (NodeList)xpath.evaluate("/layerGroup/publishables/published", document, XPathConstants.NODESET);
						for(int i = 0; i < result.getLength(); i++) {
							Node n = result.item(i);
							
							
							String name = (String)xpath.evaluate("name/text()", n, XPathConstants.STRING);
							String type = (String)xpath.evaluate("@type", n, XPathConstants.STRING);
							
							switch(type) {
								case "layer":
									layers.add(new LayerRef(name, false));
									break;
								case "layerGroup":
									layers.add(new LayerRef(name, true));
									break;
								default:
									throw new IllegalArgumentException("unknown published type: " + type + ", name: " + name);
							}
							
							
						}
						
						Node titleNode = (Node)xpath.evaluate("/layerGroup/title", document, XPathConstants.NODE);
						String title = titleNode == null ? null : titleNode.getTextContent();
						
						Node abstractNode = (Node)xpath.evaluate("/layerGroup/abstractTxt", document, XPathConstants.NODE);
						String abstr = abstractNode == null ? null : abstractNode.getTextContent();
						
						future.complete(Optional.of(new LayerGroup(layerGroupName, title, abstr, Collections.unmodifiableList(layers))));
					} else {
						future.complete(Optional.empty());
					}
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
		
		get(getLayerGroupsPath(workspace)).whenComplete((optionalDocument, t) -> {
			if(t != null) {
				future.completeExceptionally(t);
			} else {
				try {		
					Document document = optionalDocument.get();
					List<CompletableFuture<LayerGroup>> retval = new ArrayList<>();
					
					XPath xpath = XPathFactory.newInstance().newXPath();
					NodeList result = (NodeList)xpath.evaluate("/layerGroups/layerGroup/name/text()", document, XPathConstants.NODESET);
					for(int i = 0; i < result.getLength(); i++) {
						Node n = result.item(i);
						retval.add(getLayerGroup(workspace, n.getTextContent()).thenApply(this::present));
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
	public CompletableFuture<Void> putLayerGroup(Workspace workspace, LayerGroup layerGroup) {
		try {
			return put(getLayerGroupPath(workspace, layerGroup), getLayerGroupDocument(workspace, layerGroup));
		} catch(Exception e) {
			return failure(e);
		}
	}
	
	@Override
	public CompletableFuture<Void> postLayerGroup(Workspace workspace, LayerGroup layerGroup) {
		try {
			return post(getLayerGroupsPath(workspace), getLayerGroupDocument(workspace, layerGroup));
		} catch(Exception e) {
			return failure(e);
		}
	}

	private byte[] getLayerGroupDocument(Workspace workspace, LayerGroup layerGroup) throws FactoryConfigurationError, XMLStreamException, IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		XMLOutputFactory of = XMLOutputFactory.newInstance();
		XMLStreamWriter sw = of.createXMLStreamWriter(os);
		sw.writeStartDocument();
		sw.writeStartElement("layerGroup");
			sw.writeStartElement("name");
				sw.writeCharacters(layerGroup.getName());
			sw.writeEndElement();
			
			sw.writeStartElement("mode");
				sw.writeCharacters("NAMED");
			sw.writeEndElement();
			
			String title = layerGroup.getTitle();
			if(title != null) {
				sw.writeStartElement("title");
					sw.writeCharacters(title);
				sw.writeEndElement();
			}
			
			String abstr = layerGroup.getAbstract();
			if(abstr != null) {
				sw.writeStartElement("abstractTxt");
					sw.writeCharacters(abstr);
				sw.writeEndElement();
			}
			
			sw.writeStartElement("publishables");
			for(LayerRef layerRef : layerGroup.getLayers()) {
				sw.writeStartElement("published");
				sw.writeAttribute("type", layerRef.isGroup() ? "layerGroup" : "layer");
					sw.writeStartElement("name");
						// layerGroup references without workspace prefix are not correctly resolved
						sw.writeCharacters(workspace.getName() + ":" + layerRef.getLayerId());
					sw.writeEndElement();
				sw.writeEndElement();
			}
			sw.writeEndElement();
			
		sw.writeEndElement();
		sw.writeEndDocument();
		sw.close();
		
		os.close();
		
		return os.toByteArray();		
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

	@Override
	public CompletableFuture<Optional<ServiceSettings>> getServiceSettings(Workspace workspace, ServiceType serviceType) {
		CompletableFuture<Optional<ServiceSettings>> future = new CompletableFuture<>();
		
		get(getServiceSettingsPath(workspace, serviceType)).whenComplete((optionalDocument, t) -> {
			if(t != null) {
				future.completeExceptionally(t);
			} else {
				try {
					if(optionalDocument.isPresent()) {
						Document document = optionalDocument.get();						
						XPath xpath = XPathFactory.newInstance().newXPath();
						
						Node titleNode = (Node)xpath.evaluate(serviceType.name().toLowerCase() + "/title", document, XPathConstants.NODE);
						String title = titleNode == null ? null : titleNode.getTextContent();
						
						Node abstractNode = (Node)xpath.evaluate(serviceType.name().toLowerCase() + "/abstrct", document, XPathConstants.NODE);
						String abstr = abstractNode == null ? null : abstractNode.getTextContent();
						
						NodeList keywordNodes = (NodeList)xpath.evaluate(serviceType.name().toLowerCase() + "/keywords/string", document, XPathConstants.NODESET);
						List<String> keywords;
						if(keywordNodes != null && keywordNodes.getLength() > 0) {
							keywords = new ArrayList<>();
							for(int i = 0; i < keywordNodes.getLength(); i++) {
								keywords.add(keywordNodes.item(i).getTextContent());
							}
						} else {
							keywords = null;
						}
						
						future.complete(Optional.of(new ServiceSettings(title, abstr, keywords)));
					} else {					
						future.complete(Optional.empty());
					}
				} catch(Exception e) {
					future.completeExceptionally(e);
				}
			}
		});

		return future;
	}
	
	private String getWorkspaceSettingsPath(Workspace workspace) {
		return getWorkspacePath(workspace) + "/settings";
	}
	
	@Override
	public CompletableFuture<WorkspaceSettings> getWorkspaceSettings(Workspace workspace) {
		CompletableFuture<WorkspaceSettings> future = new CompletableFuture<>();
		
		get(getWorkspaceSettingsPath(workspace)).whenComplete((optionalDocument, t) -> {
			if(t != null) {
				future.completeExceptionally(t);
			} else {
				try {
					Document document = optionalDocument.get();
					XPath xpath = XPathFactory.newInstance().newXPath();
					
					Node addressNode = (Node)xpath.evaluate("settings/contact/address", document, XPathConstants.NODE);
					String address = addressNode == null ? null : addressNode.getTextContent();
					
					Node addressCityNode = (Node)xpath.evaluate("settings/contact/addressCity", document, XPathConstants.NODE);
					String city = addressCityNode == null ? null : addressCityNode.getTextContent();
					
					Node addressCountryNode = (Node)xpath.evaluate("settings/contact/addressCountry", document, XPathConstants.NODE);
					String country = addressCountryNode == null ? null : addressCountryNode.getTextContent();
					
					Node addressPostalCodeNode = (Node)xpath.evaluate("settings/contact/addressPostalCode", document, XPathConstants.NODE);
					String zipcode = addressPostalCodeNode == null ? null : addressPostalCodeNode.getTextContent();
					
					Node addressStateNode = (Node)xpath.evaluate("settings/contact/addressState", document, XPathConstants.NODE);
					String state = addressStateNode == null ? null : addressStateNode.getTextContent();
					
					Node addressTypeNode = (Node)xpath.evaluate("settings/contact/addressType", document, XPathConstants.NODE);
					String addressType = addressTypeNode == null ? null : addressTypeNode.getTextContent();
					
					Node contactEmailNode = (Node)xpath.evaluate("settings/contact/contactEmail", document, XPathConstants.NODE);
					String email = contactEmailNode == null ? null : contactEmailNode.getTextContent();
					
					Node contactFacsimileNode = (Node)xpath.evaluate("settings/contact/contactFacsimile", document, XPathConstants.NODE);
					String fax = contactFacsimileNode == null ? null : contactFacsimileNode.getTextContent();
					
					Node contactOrganizationNode = (Node)xpath.evaluate("settings/contact/contactOrganization", document, XPathConstants.NODE);
					String organization = contactOrganizationNode == null ? null : contactOrganizationNode.getTextContent();
					
					Node contactPersonNode = (Node)xpath.evaluate("settings/contact/contactPerson", document, XPathConstants.NODE);
					String contact = contactPersonNode == null ? null : contactPersonNode.getTextContent();
					
					Node contactPositionNode = (Node)xpath.evaluate("settings/contact/contactPosition", document, XPathConstants.NODE);
					String position = contactPositionNode == null ? null : contactPositionNode.getTextContent();
					
					Node contactVoiceNode = (Node)xpath.evaluate("settings/contact/contactVoice", document, XPathConstants.NODE);
					String telephone = contactVoiceNode == null ? null : contactVoiceNode.getTextContent();
					
					future.complete(new WorkspaceSettings(contact, organization, position, addressType, 
						address, city, state, zipcode, country, telephone, fax, email));					
				} catch(Exception e) {
					future.completeExceptionally(e);
				}
			}
		});
		
		return future;
	}

	@Override
	public CompletableFuture<Void> putWorkspaceSettings(Workspace workspace, WorkspaceSettings workspaceSettings) {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			
			XMLOutputFactory of = XMLOutputFactory.newInstance();
			XMLStreamWriter sw = of.createXMLStreamWriter(os);			
			sw.writeStartDocument();
			
			sw.writeStartElement("settings");
				sw.writeStartElement("contact");
					sw.writeStartElement("id");						
						sw.writeCharacters("contact");
					sw.writeEndElement();
						
					String address = workspaceSettings.getAddress();
					if(address != null) {
						sw.writeStartElement("address");
							sw.writeCharacters(address);
						sw.writeEndElement();
					}
					
					String city = workspaceSettings.getCity();
					if(city != null) {
						sw.writeStartElement("addressCity");
							sw.writeCharacters(city);
						sw.writeEndElement();
					}
					
					String country = workspaceSettings.getCountry();
					if(country != null) {
						sw.writeStartElement("addressCountry");
							sw.writeCharacters(country);
						sw.writeEndElement();
					}
					
					String zipcode = workspaceSettings.getZipcode();
					if(zipcode != null) {
						sw.writeStartElement("addressPostalCode");
							sw.writeCharacters(zipcode);
						sw.writeEndElement();
					}
					
					String state = workspaceSettings.getState();
					if(state != null) {
						sw.writeStartElement("addressState");
							sw.writeCharacters(state);
						sw.writeEndElement();
					}
					
					String addressType = workspaceSettings.getAddressType();
					if(addressType != null) {
						sw.writeStartElement("addressType");
							sw.writeCharacters(addressType);
						sw.writeEndElement();
					}
					
					String email = workspaceSettings.getEmail();
					if(email != null) {
						sw.writeStartElement("contactEmail");
							sw.writeCharacters(email);
						sw.writeEndElement();
					}
					
					String fax = workspaceSettings.getFax();
					if(fax != null) {
						sw.writeStartElement("contactFacsimile");
							sw.writeCharacters(fax);
						sw.writeEndElement();
					}
					
					String organization = workspaceSettings.getOrganization();
					if(organization != null) {
						sw.writeStartElement("contactOrganization");
							sw.writeCharacters(organization);
						sw.writeEndElement();
					}
					
					String contact = workspaceSettings.getContact();
					if(contact != null) {
						sw.writeStartElement("contactPerson");
							sw.writeCharacters(contact);
						sw.writeEndElement();
					}
					
					String position = workspaceSettings.getPosition();
					if(position != null) {
						sw.writeStartElement("contactPosition");
							sw.writeCharacters(position);
						sw.writeEndElement();
					}
					
					String telephone = workspaceSettings.getTelephone();
					if(telephone != null) {
						sw.writeStartElement("contactVoice");
							sw.writeCharacters(telephone);
						sw.writeEndElement();
					}						
				sw.writeEndElement();
			sw.writeEndElement();
			
			sw.writeEndDocument();
			sw.close();
			
			return put(getWorkspaceSettingsPath(workspace), os.toByteArray());
		} catch(Exception e) {
			return failure(e);
		}
	}
	
	@Override
	public CompletableFuture<Optional<Workspace>> getWorkspace(String workspaceId) {
		CompletableFuture<Optional<Workspace>> future = new CompletableFuture<>();
		
		get(getWorkspacePath(workspaceId)).whenComplete((optionalDocument, t) -> {
			if(t != null) {
				future.completeExceptionally(t);
			} else {
				try {
					if(optionalDocument.isPresent()) {
						future.complete(Optional.of(new Workspace(workspaceId)));
					} else {
						future.complete(Optional.empty());
					}
				} catch(Exception e) {
					future.completeExceptionally(e);
				}
			}
		});
		
		return future;
	}
}

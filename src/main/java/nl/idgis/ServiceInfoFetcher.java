package nl.idgis;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class ServiceInfoFetcher {
	
	private final DataSource dataSource;
	
	private final String serviceQuery;
	
	private final ObjectMapper om = new ObjectMapper();
	
	public ServiceInfoFetcher(@Autowired DataSource dataSource) throws Exception {
		this.dataSource = dataSource;
		
		final String resourceName = "service.sql";
		final InputStream is = ServiceInfoFetcher.class.getResourceAsStream(resourceName);		
		if (is == null) {
			throw new IllegalStateException("resource is missing: " + resourceName);
		}
		serviceQuery = IOUtils.toString(is, "utf-8");
	}
	
	private static JsonNode findChildLayerById(JsonNode layer, int id) {
		JsonNode layers = layer.get("layers");
		if (layers == null) {
			throw new IllegalStateException("layers field is missing");
		}
		
		for (JsonNode childLayer : layers) {
			JsonNode idNode = childLayer.get("id");
			if (idNode == null) {
				throw new IllegalStateException("id field is missing");
			}
		
			if (idNode.asInt() == id) {
				return childLayer;
			}
		}
		
		return null;
	}
	
	private static void removeAllLayerIds(ObjectNode layer) {
		layer.remove("id");
		JsonNode layers = layer.get("layers");
		if (layers != null) {
			for (JsonNode childLayer : layers) {
				removeAllLayerIds((ObjectNode)childLayer);
			}
		}
	}
	
	private static void makeLayerNamesUnique(ObjectNode layer) {
		makeLayerNamesUnique(layer, new HashSet<>());
	}
	
	private static void makeLayerNamesUnique(ObjectNode layer, Set<String> layerNames) {
		JsonNode name = layer.get("name");
		if (name == null) {
			throw new IllegalStateException("name field is missing");
		}
		
		String nameText = name.asText();				
		if (layerNames.contains(nameText)) {
			int postFix = 2;
			String newName;
			
			do {
				newName = nameText + "-" + postFix;
				postFix++;
			} while (layerNames.contains(newName));
			
			layerNames.add(newName);
			((ObjectNode)layer).put("name", newName);
		} else {
			layerNames.add(nameText);
		}
		
		JsonNode layers = layer.get("layers");
		if (layers != null) {
			for (JsonNode childLayer : layers) {
				makeLayerNamesUnique((ObjectNode)childLayer, layerNames);
			}
		}
	}
	
	public List<JsonNode> fetchAllServiceInfos() throws SQLException, IOException {
		List<JsonNode> serviceInfos = new ArrayList<>();
		fetchAllServiceInfos(this::serviceInfoBuilder, serviceInfo -> {
			serviceInfos.add(postProcessServiceInfo(serviceInfo));
		});
		return serviceInfos;
	}
	
	private interface DataSourceConsumer<T> {
		
		void accept(T t) throws SQLException, IOException;
	}
	
	private <T> void fetchAllServiceInfos(ServiceInfoBuilder<T> builder, DataSourceConsumer<? super T> consumer) throws SQLException, IOException {
		try (Connection c = DataSourceUtils.getConnection(dataSource); 
				PreparedStatement stmt = c.prepareStatement(serviceQuery)) {
			
			T serviceInfo = null;
			String lastServiceName = null;
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					String serviceName = rs.getString(1);
					Integer[] anchestors = (Integer[])rs.getArray(2).getArray();
					JsonNode layers = om.readTree(rs.getBinaryStream(3));

					if (!serviceName.equals(lastServiceName)) {
						if (serviceInfo != null) {
							consumer.accept(serviceInfo);
						}

						lastServiceName = serviceName;
						serviceInfo = null;
					}
					
					serviceInfo = builder.accept(serviceInfo, layers, anchestors);
				}
				
				if (serviceInfo != null) {
					consumer.accept(serviceInfo);
				}
			}
		}
	}
	
	private static JsonNode postProcessServiceInfo(JsonNode root) {
		JsonNode layers = root.get("layers");
		if (layers == null) {
			throw new IllegalStateException("layers field is missing at root level");
		}
		
		if (layers.size() != 1) {
			throw new IllegalStateException("Unexpected number (!= 1) of root layers: " + layers.size());
		}
		
		ObjectNode rootLayer = (ObjectNode)layers.get(0);
		rootLayer.remove("type");
		removeAllLayerIds(rootLayer);
		makeLayerNamesUnique(rootLayer);
		
		return rootLayer;
	}
	
	public Optional<JsonNode> fetchServiceInfo(String serviceName) throws SQLException, IOException {
		JsonNode root = fetchServiceInfo(serviceName, this::serviceInfoBuilder);
		
		if (root == null) {
			return Optional.empty();
		}
		
		return Optional.of(postProcessServiceInfo(root));
	}
	
	@FunctionalInterface
	private interface ServiceInfoBuilder<T> {

		T accept(T serviceInfo, JsonNode layers, Integer[] anchestors);
	}
	
	private final ObjectNode serviceInfoBuilder(ObjectNode serviceInfo, JsonNode layers, Integer[] anchestors) {
		if (anchestors.length == 0) {
			serviceInfo = om.createObjectNode();
			serviceInfo.set("layers", layers);
		} else {
			if (serviceInfo == null) {
				throw new IllegalStateException("no service info available");
			}
			
			ObjectNode currentLayer = serviceInfo;
			for (int anchestor : anchestors) {
				JsonNode child = findChildLayerById(currentLayer, anchestor);
				if (child == null) {
					throw new IllegalStateException("failed to traverse anchestors");
				}
				currentLayer = (ObjectNode)child;
			}
			
			currentLayer.set("layers", layers);
		}
		
		return serviceInfo;
	};
	
	private <T> T fetchServiceInfo(String serviceName, ServiceInfoBuilder<T> builder) throws SQLException, IOException {
		T serviceInfo = null;
		
		try (Connection c = DataSourceUtils.getConnection(dataSource); 
				PreparedStatement stmt = c.prepareStatement("select s.* from (" + serviceQuery + ") s where s.service_name = ?")) {
			
			stmt.setString(1, serviceName);
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					Integer[] anchestors = (Integer[])rs.getArray(2).getArray();
					JsonNode layers = om.readTree(rs.getBinaryStream(3));
					serviceInfo = builder.accept(serviceInfo, layers, anchestors);
				}
			}
		}
		
		return serviceInfo;
	}
}

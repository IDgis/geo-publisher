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
public class StagingRepository {
	
	private final DataSource dataSource;
	
	private final String serviceQuery;
	
	private final ObjectMapper om = new ObjectMapper();
	
	public StagingRepository(@Autowired DataSource dataSource) throws Exception {
		this.dataSource = dataSource;
		
		final String resourceName = "service.sql";
		final InputStream is = StagingRepository.class.getResourceAsStream(resourceName);
		if (is == null) {
			throw new IllegalStateException("resource is missing: " + resourceName);
		}
		serviceQuery = IOUtils.toString(is, "utf-8");
	}
	
	private static JsonNode findChildLayerById(JsonNode layer, int internalId) {
		JsonNode layers = layer.get("layers");
		if (layers == null) {
			throw new IllegalStateException("layers field is missing");
		}
		
		for (JsonNode childLayer : layers) {
			JsonNode idNode = childLayer.get("internal_id");
			if (idNode == null) {
				throw new IllegalStateException("internal_id field is missing");
			}
		
			if (idNode.asInt() == internalId) {
				return childLayer;
			}
		}
		
		return null;
	}
	
	private static void removeAllInternalLayerIds(ObjectNode layer) {
		layer.remove("internal_id");
		JsonNode layers = layer.get("layers");
		if (layers != null) {
			for (JsonNode childLayer : layers) {
				removeAllInternalLayerIds((ObjectNode)childLayer);
			}
		}
	}
	
	public List<JsonNode> getAllServiceInfos() throws SQLException, IOException {
		List<JsonNode> serviceInfos = new ArrayList<>();
		getAllServiceInfos(this::serviceInfoBuilder, serviceInfo -> {
			serviceInfos.add(postProcessServiceInfo(serviceInfo));
		});
		return serviceInfos;
	}
	
	private interface DataSourceConsumer<T> {
		
		void accept(T t) throws SQLException, IOException;
	}
	
	private <T> void getAllServiceInfos(ServiceInfoBuilder<T> builder, DataSourceConsumer<? super T> consumer) throws SQLException, IOException {
		try (Connection c = DataSourceUtils.getConnection(dataSource); 
				PreparedStatement stmt = c.prepareStatement(serviceQuery)) {
			
			T serviceInfo = null;
			String lastServiceId = null;
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					String serviceId = rs.getString(1);
					Integer[] anchestors = (Integer[])rs.getArray(2).getArray();
					JsonNode layers = om.readTree(rs.getBinaryStream(3));

					if (!serviceId.equals(lastServiceId)) {
						if (serviceInfo != null) {
							consumer.accept(serviceInfo);
						}

						lastServiceId = serviceId;
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
		removeAllInternalLayerIds(rootLayer);
		Utils.makeLayerNamesUnique(rootLayer);
		
		return rootLayer;
	}
	
	public Optional<JsonNode> getServiceInfo(String id) throws SQLException, IOException {
		JsonNode root = getServiceInfo(id, this::serviceInfoBuilder);
		
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
	
	private <T> T getServiceInfo(String serviceId, ServiceInfoBuilder<T> builder) throws SQLException, IOException {
		T serviceInfo = null;
		
		try (Connection c = DataSourceUtils.getConnection(dataSource); 
				PreparedStatement stmt = c.prepareStatement("select s.* from (" + serviceQuery + ") s where s.service_id = ?")) {
			
			stmt.setString(1, serviceId);
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

	public List<JsonNode> getAllStyleRefs() throws SQLException {
		List<JsonNode> styleRefs = new ArrayList<>();

		try (Connection c = DataSourceUtils.getConnection(dataSource);
			 PreparedStatement stmt = c.prepareStatement(
					 "select s.identification, s.name " +
						 "from publisher.style s")) {

			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					ObjectNode styleRef = om.createObjectNode();
					styleRef.put("id", rs.getString(1));
					styleRef.put("name", rs.getString(2));
					styleRefs.add(styleRef);
				}
			}
		}

		return styleRefs;
	}

	public Optional<byte[]> getStyleBody(String id) throws SQLException {
		try (Connection c = DataSourceUtils.getConnection(dataSource);
			 PreparedStatement stmt = c.prepareStatement(
					 "select s.definition " +
						 "from publisher.style s " +
						 "where s.identification = ?")) {

			stmt.setString(1, id);

			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					return Optional.of(rs.getBytes(1));
				}
			}
		}

		return Optional.empty();
	}
}

package nl.idgis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class PublicationRepository {

    private final DataSource dataSource;

    private final ObjectMapper om = new ObjectMapper();

    public PublicationRepository(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private void modifyContent(ObjectNode node) {
        modifyContent(node, false);
    }

    private void modifyContent(ObjectNode node, boolean isLayer) {
        JsonNode layers = node.get("layers");
        if (layers != null) {
            if (!layers.isArray()) {
                throw new IllegalStateException("layers value is not an array");
            }

            ArrayNode layersReplacement = om.createArrayNode();
            for (JsonNode child : layers) {
                ObjectNode childLayer = ((ObjectNode)child.get("layer"));
                if (childLayer == null) {
                    throw new IllegalStateException("layer attribute is missing");
                }
                modifyContent(childLayer, true);
                layersReplacement.add(childLayer);
            }

            node.put("layers", layersReplacement);
            if (isLayer) {
                node.put("type", "group");
            }
        }
    }

    public List<JsonNode> getAllServiceInfos(String environment) throws SQLException, IOException {
        List<JsonNode> serviceInfos = new ArrayList<>();

        try (Connection c = DataSourceUtils.getConnection(dataSource);
             PreparedStatement stmt = c.prepareStatement(
                     "select ps.content " +
                         "from publisher.published_service ps " +
                         "join publisher.environment e on e.id = ps.environment_id " +
                         "where e.identification = ?")) {

            stmt.setString(1, environment);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ObjectNode content = (ObjectNode)om.readTree(rs.getBinaryStream(1));
                    modifyContent(content);
                    serviceInfos.add(content);
                }
            }
        }

        return serviceInfos;
    }


    public Optional<JsonNode> getServiceInfo(String environment, String id) throws SQLException, IOException {
        try (Connection c = DataSourceUtils.getConnection(dataSource);
             PreparedStatement stmt = c.prepareStatement(
                     "select ps.content " +
                         "from publisher.published_service ps " +
                         "join publisher.environment e on e.id = ps.environment_id " +
                         "join publisher.service s on s.id = ps.service_id " +
                         "join publisher.generic_layer gl on gl.id = s.generic_layer_id " +
                         "where e.identification = ? and gl.identification = ?")) {

            stmt.setString(1, environment);
            stmt.setString(2, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ObjectNode content = (ObjectNode)om.readTree(rs.getBinaryStream(1));
                    modifyContent(content);
                    return Optional.of(content);
                }
            }
        }

        return Optional.empty();
    }

    public List<JsonNode> getAllStyleRefs(String environment) throws SQLException {
        List<JsonNode> styleRefs = new ArrayList<>();

        try (Connection c = DataSourceUtils.getConnection(dataSource);
             PreparedStatement stmt = c.prepareStatement(
                     "select pss.identification, pss.name " +
                         "from publisher.published_service_style pss " +
                         "join publisher.published_service ps on ps.service_id = pss.service_id " +
                         "join publisher.environment e on e.id = ps.environment_id " +
                         "where e.identification = ?")) {

            stmt.setString(1, environment);

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

    public Optional<byte[]> getStyleBody(String environment, String id) throws SQLException {
        try (Connection c = DataSourceUtils.getConnection(dataSource);
             PreparedStatement stmt = c.prepareStatement(
                     "select pss.definition " +
                         "from publisher.published_service_style pss " +
                         "join publisher.published_service ps on ps.service_id = pss.service_id " +
                         "join publisher.environment e on e.id = ps.environment_id " +
                         "where e.identification = ? " +
                         "and pss.identification = ?")) {

            stmt.setString(1, environment);
            stmt.setString(2, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getBytes(1));
                }
            }
        }

        return Optional.empty();
    }
}

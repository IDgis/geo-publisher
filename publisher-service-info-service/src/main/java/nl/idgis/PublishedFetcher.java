package nl.idgis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class PublishedFetcher {

    private final DataSource dataSource;

    private final ObjectMapper om = new ObjectMapper();

    public PublishedFetcher(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<JsonNode> fetchAllServiceInfos(String environment) throws SQLException, IOException {
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
                    JsonNode content = om.readTree(rs.getBinaryStream(1));
                    serviceInfos.add(content);
                }
            }
        }

        return serviceInfos;
    }


    public Optional<JsonNode> fetchServiceInfo(String environment, String id) throws SQLException, IOException {
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
                    return Optional.of(om.readTree(rs.getBinaryStream(1)));
                }
            }
        }

        return Optional.empty();
    }
}

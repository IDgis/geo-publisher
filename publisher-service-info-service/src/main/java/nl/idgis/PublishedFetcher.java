package nl.idgis;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class PublishedFetcher {

    private final DataSource dataSource;

    public PublishedFetcher(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<JsonNode> fetchAllServiceInfos(String environment) {
        return Collections.emptyList();
    }


    public Optional<JsonNode> fetchServiceInfo(String environment, String id) {
        return Optional.empty();
    }
}

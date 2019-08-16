package nl.idgis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/published/{environment}/services")
public class PublishedController {

    private final PublishedFetcher publishedFetcher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PublishedController(@Autowired PublishedFetcher publishedFetcher) {
        this.publishedFetcher = publishedFetcher;
    }

    @RequestMapping
    public JsonNode services(@PathVariable("environment") String environment) throws Exception {
        ArrayNode services = objectMapper.createArrayNode();
        for (JsonNode serviceInfo : publishedFetcher.fetchAllServiceInfos(environment)) {
            services.add(serviceInfo);
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.set("services", services);

        return root;
    }

    @RequestMapping("/{id}")
    public JsonNode service(@PathVariable("environment") String environment, @PathVariable("id") String id) throws Exception {
        return publishedFetcher.fetchServiceInfo(environment, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}

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
@RequestMapping("/publication/{environment}/services")
public class PublicationController {

    private final PublicationFetcher publicationFetcher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PublicationController(@Autowired PublicationFetcher publicationFetcher) {
        this.publicationFetcher = publicationFetcher;
    }

    @RequestMapping
    public JsonNode services(@PathVariable("environment") String environment) throws Exception {
        ArrayNode services = objectMapper.createArrayNode();
        for (JsonNode serviceInfo : publicationFetcher.fetchAllServiceInfos(environment)) {
            services.add(serviceInfo);
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.set("services", services);

        return root;
    }

    @RequestMapping("/{id}")
    public JsonNode service(@PathVariable("environment") String environment, @PathVariable("id") String id) throws Exception {
        return publicationFetcher.fetchServiceInfo(environment, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}

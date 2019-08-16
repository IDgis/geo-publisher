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
@RequestMapping("/staging/services")
public class StagingController {

    private final StagingFetcher stagingFetcher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public StagingController(@Autowired StagingFetcher serviceInfoFetcher) {
        this.stagingFetcher = serviceInfoFetcher;
    }

    @RequestMapping
    public JsonNode services() throws Exception {
        ArrayNode services = objectMapper.createArrayNode();
        for (JsonNode serviceInfo : stagingFetcher.fetchAllServiceInfos()) {
            services.add(serviceInfo);
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.set("services", services);

        return root;
    }

    @RequestMapping("/{id}")
    public JsonNode service(@PathVariable("id") String id) throws Exception {
        return stagingFetcher.fetchServiceInfo(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}

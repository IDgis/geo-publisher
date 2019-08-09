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
public class ServiceInfoController {

    private final ServiceInfoFetcher serviceInfoFetcher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ServiceInfoController(@Autowired ServiceInfoFetcher serviceInfoFetcher) {
        this.serviceInfoFetcher = serviceInfoFetcher;
    }

    @RequestMapping("/services")
    public JsonNode services() throws Exception {
        ArrayNode services = objectMapper.createArrayNode();
        for (JsonNode serviceInfo : serviceInfoFetcher.fetchAllServiceInfos()) {
            services.add(serviceInfo);
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.set("services", services);

        return root;
    }

    @RequestMapping("/services/{id}")
    public JsonNode service(@PathVariable("id") String id) throws Exception {
        return serviceInfoFetcher.fetchServiceInfo(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}

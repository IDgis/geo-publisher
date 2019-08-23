package nl.idgis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/publication/{environment}")
public class PublicationController {

    private final PublicationRepository publicationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PublicationController(@Autowired PublicationRepository publicationRepository) {
        this.publicationRepository = publicationRepository;
    }

    @RequestMapping("/services")
    public JsonNode services(@PathVariable("environment") String environment) throws Exception {
        ArrayNode services = objectMapper.createArrayNode();
        for (JsonNode serviceInfo : publicationRepository.fetchAllServiceInfos(environment)) {
            services.add(serviceInfo);
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.set("services", services);

        return root;
    }

    @RequestMapping("/services/{id}")
    public JsonNode service(@PathVariable("environment") String environment, @PathVariable("id") String id) throws Exception {
        return publicationRepository.fetchServiceInfo(environment, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @RequestMapping("/styles")
    public JsonNode styles(@PathVariable("environment") String environment) throws Exception {
        ArrayNode styleRefs = objectMapper.createArrayNode();
        for (JsonNode styleRef : publicationRepository.fetchAllStyleRefs(environment)) {
            styleRefs.add(styleRef);
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.set("styles", styleRefs);

        return root;
    }

    @RequestMapping("/styles/{id}")
    public ResponseEntity<Object> style(@PathVariable("environment") String environment, @PathVariable("id") String id) throws Exception {
        return publicationRepository.fetchStyleBody(environment, id)
                .map(styleBody ->
                        ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_XML)
                            .<Object>body(styleBody))
                .orElseGet(() ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("Unknown style: " + id));
    }
}

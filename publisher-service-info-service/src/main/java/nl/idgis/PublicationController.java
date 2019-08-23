package nl.idgis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        for (JsonNode serviceInfo : publicationRepository.getAllServiceInfos(environment)) {
            services.add(serviceInfo);
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.set("services", services);

        return root;
    }

    @RequestMapping("/services/{id}")
    public ResponseEntity<JsonNode> service(@PathVariable("environment") String environment, @PathVariable("id") String id) throws Exception {
        return ResponseEntity.of(publicationRepository.getServiceInfo(environment, id));
    }

    @RequestMapping("/styles")
    public JsonNode styles(@PathVariable("environment") String environment) throws Exception {
        ArrayNode styleRefs = objectMapper.createArrayNode();
        for (JsonNode styleRef : publicationRepository.getAllStyleRefs(environment)) {
            styleRefs.add(styleRef);
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.set("styles", styleRefs);

        return root;
    }

    @RequestMapping("/styles/{id}")
    public ResponseEntity<byte[]> style(@PathVariable("environment") String environment, @PathVariable("id") String id) throws Exception {
        return publicationRepository.getStyleBody(environment, id)
                .map(styleBody ->
                        ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_XML)
                            .body(styleBody))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

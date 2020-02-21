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
@RequestMapping("/staging")
public class StagingController {

    private final StagingRepository stagingRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public StagingController(@Autowired StagingRepository stagingRepository) {
        this.stagingRepository = stagingRepository;
    }

    @RequestMapping("/services")
    public JsonNode services() throws Exception {
        ArrayNode services = objectMapper.createArrayNode();
        for (JsonNode serviceInfo : stagingRepository.getAllServiceInfos()) {
            services.add(serviceInfo);
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.set("services", services);

        return root;
    }

    @RequestMapping("/services/{id}")
    public ResponseEntity<JsonNode> service(@PathVariable("id") String id) throws Exception {
        return ResponseEntity.of(stagingRepository.getServiceInfo(id));
    }

    @RequestMapping("/styles")
    public JsonNode styles() throws Exception {
        ArrayNode styleRefs = objectMapper.createArrayNode();

        ObjectNode root = objectMapper.createObjectNode();
        for (JsonNode styleRef : stagingRepository.getAllStyleRefs()) {
            styleRefs.add(styleRef);
        }
        root.set("styles", styleRefs);

        return root;
    }

    @RequestMapping("/styles/{id}")
    public ResponseEntity<byte[]> style(@PathVariable("id") String id) throws Exception {
        return stagingRepository.getStyleBody(id)
                .map(styleBody ->
                    ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_XML)
                        .body(styleBody))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

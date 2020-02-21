package nl.idgis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashSet;
import java.util.Set;

public class Utils {

    public static void makeLayerNamesUnique(ObjectNode layer) {
        makeLayerNamesUnique(layer, new HashSet<>());
    }

    private static void makeLayerNamesUnique(ObjectNode layer, Set<String> layerNames) {
        JsonNode name = layer.get("name");
        if (name == null) {
            throw new IllegalStateException("name field is missing");
        }

        String nameText = name.asText();
        if (layerNames.contains(nameText)) {
            int postFix = 2;
            String newName;

            do {
                newName = nameText + "-" + postFix;
                postFix++;
            } while (layerNames.contains(newName));

            layerNames.add(newName);
            ((ObjectNode)layer).put("name", newName);
        } else {
            layerNames.add(nameText);
        }

        JsonNode layers = layer.get("layers");
        if (layers != null) {
            for (JsonNode childLayer : layers) {
                makeLayerNamesUnique((ObjectNode)childLayer, layerNames);
            }
        }
    }
}

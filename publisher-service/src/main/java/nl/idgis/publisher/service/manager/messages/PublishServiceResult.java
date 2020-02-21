package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;
import java.util.Optional;

public class PublishServiceResult implements Serializable {

    private static final long serialVersionUID = -6528382192513260897L;

    private final String previousEnvironmentId;

    private final String currentEnvironmentId;

    public PublishServiceResult(String previousEnvironmentId, String currentEnvironmentId) {
        this.previousEnvironmentId = previousEnvironmentId;
        this.currentEnvironmentId = currentEnvironmentId;
    }

    public Optional<String> getPreviousEnvironmentId() {
        return Optional.ofNullable(previousEnvironmentId);
    }

    public Optional<String> getCurrentEnvironmentId() {
        return Optional.ofNullable(currentEnvironmentId);
    }

    @Override
    public String toString() {
        return "PublishServiceResult{" +
                "previousEnvironmentId='" + previousEnvironmentId + '\'' +
                ", currentEnvironmentId='" + currentEnvironmentId + '\'' +
                '}';
    }
}

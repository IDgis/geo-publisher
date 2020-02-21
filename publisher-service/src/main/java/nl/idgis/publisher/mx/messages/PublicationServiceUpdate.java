package nl.idgis.publisher.mx.messages;

public class PublicationServiceUpdate extends AbstractServiceUpdate {

    private final String environmentId;

    public PublicationServiceUpdate(ServiceUpdateType type, String serviceId, String environmentId) {
        super(type, serviceId);

        this.environmentId = environmentId;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    @Override
    public String toString() {
        return "PublicationServiceUpdate{" +
                "environmentId='" + environmentId + '\'' +
                ", type=" + type +
                ", serviceId='" + serviceId + '\'' +
                '}';
    }
}

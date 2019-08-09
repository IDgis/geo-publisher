package nl.idgis.publisher.mx.messages;

public class StagingServiceUpdate {

    private final ServiceUpdateType type;

    private final String serviceId;

    public StagingServiceUpdate(ServiceUpdateType type, String serviceId) {
        this.type = type;
        this.serviceId = serviceId;
    }

    public ServiceUpdateType getType() {
        return type;
    }

    public String getServiceId() {
        return serviceId;
    }

    @Override
    public String toString() {
        return "StagingServiceUpdate{" +
                "type=" + type +
                ", serviceId='" + serviceId + '\'' +
                '}';
    }
}

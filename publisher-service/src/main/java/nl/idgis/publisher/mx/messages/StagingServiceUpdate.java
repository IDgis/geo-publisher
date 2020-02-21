package nl.idgis.publisher.mx.messages;

public class StagingServiceUpdate extends AbstractServiceUpdate {

    public StagingServiceUpdate(ServiceUpdateType type, String serviceId) {
        super(type, serviceId);
    }

    @Override
    public String toString() {
        return "StagingServiceUpdate{" +
                "type=" + type +
                ", serviceId='" + serviceId + '\'' +
                '}';
    }
}

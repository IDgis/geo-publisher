package nl.idgis.publisher.mx.messages;

public abstract class AbstractServiceUpdate {

    protected final ServiceUpdateType type;

    protected final String serviceId;

    public AbstractServiceUpdate(ServiceUpdateType type, String serviceId) {
        this.type = type;
        this.serviceId = serviceId;
    }

    public ServiceUpdateType getType() {
        return type;
    }

    public String getServiceId() {
        return serviceId;
    }
}

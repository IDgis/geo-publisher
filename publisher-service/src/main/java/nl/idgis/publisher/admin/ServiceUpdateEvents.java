package nl.idgis.publisher.admin;

import nl.idgis.publisher.domain.web.Service;

import akka.actor.ActorRef;
import akka.actor.Props;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ServiceUpdateEvents extends AbstractAdmin {

    private Connection connection;

    private Channel channel;
    
    private static final String EXCHANGE_NAME = "pub.geoserver.staging.updates";
    
    private final ObjectMapper om = new ObjectMapper();

	public ServiceUpdateEvents(ActorRef database) {
		super(database);
	}
	
	public static Props props(ActorRef database) {
		return Props.create(ServiceUpdateEvents.class, database);
	}
	
	private void sendUpdateEvent(String type, String name) {
        try {	
            ObjectNode message = om.createObjectNode();
            message.put("type", type);
            message.put("name", name);
            
            channel.basicPublish(EXCHANGE_NAME, "", null, om.writeValueAsBytes(message));
        } catch(Exception e) {
            throw new RuntimeException("Failed to send message to RabbitMQ", e);
        }
	}
	
	@Override
	protected void preStartAdmin() {
	
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("rabbitmq");
            
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        } catch(Exception e) {
            throw new RuntimeException("Failed to connect to RabbitMQ", e);
        }
        
        onPut(Service.class, (service, serviceId) -> sendUpdateEvent("create", service.name()));
	}
}

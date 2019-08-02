package nl.idgis.publisher.admin;

import akka.actor.ActorRef;
import akka.actor.Props;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class ServiceUpdateEvents extends AbstractAdmin {

    private Connection connection;

    private Channel channel;
    
    private static final String EXCHANGE_NAME = "pub.geoserver.staging.updates";

	public ServiceUpdateEvents(ActorRef database) {
		super(database);
	}
	
	public static Props props(ActorRef database) {
		return Props.create(ServiceUpdateEvents.class, database);
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
	}
}

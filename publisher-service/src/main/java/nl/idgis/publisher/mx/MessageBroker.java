package nl.idgis.publisher.mx;

import akka.actor.UntypedActor;

import akka.actor.Props;

import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nl.idgis.publisher.mx.messages.ServiceUpdateType;
import nl.idgis.publisher.mx.messages.StagingServiceUpdate;
import scala.Option;

public class MessageBroker extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private Connection connection;

    private Channel channel;
    
    private static final String STAGING_EXCHANGE_NAME = "pub.geoserver.staging.updates";
    
    private final ObjectMapper om = new ObjectMapper();
	
	public static Props props() {
		return Props.create(MessageBroker.class);
	}

    @Override
    public void onReceive(Object msg) throws Exception {
	    if (msg instanceof StagingServiceUpdate) {
            handleStagingServiceUpdate((StagingServiceUpdate)msg);
        } else {
	        unhandled(msg);
        }
    }

    private void handleStagingServiceUpdate(StagingServiceUpdate msg) {
        sendServiceUpdate(STAGING_EXCHANGE_NAME, msg.getType(), msg.getServiceId());
    }
	
	private void sendServiceUpdate(String exchange, ServiceUpdateType type, String serviceId) {
        try {
            ObjectNode message = om.createObjectNode();
            message.put("type", type.name().toLowerCase());
            message.put("id", serviceId);

            if (log.isDebugEnabled()) {
                log.debug("publishing message, exchange: " + exchange + " body: " + om.writeValueAsString(message));
            }
            
            channel.basicPublish(exchange, "", null, om.writeValueAsBytes(message));
        } catch(Exception e) {
            throw new RuntimeException("Failed to send message to RabbitMQ", e);
        }
	}

	private void init() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("rabbitmq");

        try {
            connection = factory.newConnection();
            log.debug("connected");
        } catch(Exception e) {
            throw new RuntimeException("Failed to connect to RabbitMQ", e);
        }

        try {
            channel = connection.createChannel();
            log.debug("channel created");
        } catch(Exception e) {
            throw new RuntimeException("Failed to create to RabbitMQ channel", e);
        }

        try {
            channel.exchangeDeclare(STAGING_EXCHANGE_NAME, "fanout");
            log.debug("exchange declared: " + STAGING_EXCHANGE_NAME);
        } catch(Exception e) {
            throw new RuntimeException("Failed to declare RabbitMQ exchange", e);
        }
    }

    private void terminate() {
	    if (channel != null) {
	        try {
                channel.close();
                log.debug("channel closed");
            } catch(Exception e) {
                throw new RuntimeException("Failed to close RabbitMQ channel", e);
            }
        }

	    if (connection != null) {
	        try {
                connection.close();
                log.debug("connection closed");
            } catch(Exception e) {
                throw new RuntimeException("Failed to close RabbitMQ connection", e);
            }
        }
    }
    
	@Override
	public void preStart() {
        log.debug("begin preStart");
        init();
        log.debug("end preStart");
	}

	@Override
    public void preRestart(Throwable reason, Option<Object> message) {
        log.debug("begin preRestart");
	    terminate();
        log.debug("end preRestart");
    }

	@Override
    public void postStop() {
        log.debug("begin postStop");
        terminate();
        log.debug("end postStop");
    }
}

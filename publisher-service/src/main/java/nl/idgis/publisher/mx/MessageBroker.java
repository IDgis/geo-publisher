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
import nl.idgis.publisher.mx.messages.AbstractServiceUpdate;
import nl.idgis.publisher.mx.messages.PublicationServiceUpdate;
import nl.idgis.publisher.mx.messages.StagingServiceUpdate;
import scala.Option;

public class MessageBroker extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private Connection connection;

    private Channel channel;

    private static final String EXCHANGE_NAME_PREFIX = "pub.geoserver.";
    
    private static final String STAGING_EXCHANGE_NAME = EXCHANGE_NAME_PREFIX + "staging.updates";

    private static final String PUBLICATION_EXCHANGE_NAME_PREFIX = EXCHANGE_NAME_PREFIX + "publication.";

    private static final String PUBLICATION_EXCHANGE_NAME_POSTFIX = ".updates";
    
    private final ObjectMapper om = new ObjectMapper();
	
	public static Props props() {
		return Props.create(MessageBroker.class);
	}

    @Override
    public void onReceive(Object msg) throws Exception {
        if (msg instanceof StagingServiceUpdate) {
            sendServiceUpdate(STAGING_EXCHANGE_NAME, (StagingServiceUpdate)msg);
        } else if (msg instanceof PublicationServiceUpdate) {
            PublicationServiceUpdate serviceUpdate = (PublicationServiceUpdate)msg;
            String exchangeName = PUBLICATION_EXCHANGE_NAME_PREFIX
                    + serviceUpdate.getEnvironmentId()
                    + PUBLICATION_EXCHANGE_NAME_POSTFIX;
            sendServiceUpdate(exchangeName, serviceUpdate);
        } else {
	        unhandled(msg);
        }
    }
	
	private void sendServiceUpdate(String exchangeName, AbstractServiceUpdate serviceUpdate) {
        try {
            ObjectNode message = om.createObjectNode();
            message.put("type", serviceUpdate.getType().name().toLowerCase());
            message.put("id", serviceUpdate.getServiceId());

            if (log.isDebugEnabled()) {
                log.debug("publishing message, exchange: " + exchangeName + " body: " + om.writeValueAsString(message));
            }
            
            channel.basicPublish(exchangeName, "", null, om.writeValueAsBytes(message));
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

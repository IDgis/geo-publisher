package nl.idgis.publisher.protocol;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.typesafe.config.Config;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActorWithStash;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.io.Tcp.Event;
import akka.io.TcpMessage;
import akka.io.Tcp.ConnectionClosed;
import akka.io.Tcp.Received;
import akka.japi.Procedure;
import akka.util.ByteString;

public class SSLHandler extends UntypedActorWithStash {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Config config;
	private final boolean isServer;
	private final ActorRef connection, listener;
	
	private SSLEngine sslEngine;	
	private ByteBuffer netOutput, appInput;
	private ByteString netInput, appOutput;
	
	private static class Ack implements Event {
		
	}
	
	public SSLHandler(Config config, boolean isServer, ActorRef connection, ActorRef listener) {
		this.config = config;
		this.isServer = isServer;		
		this.connection = connection;
		this.listener = listener;
	}
	
	public static Props props(Config config, boolean isServer, ActorRef connection, ActorRef listener) {
		return Props.create(SSLHandler.class, config, isServer, connection, listener);
	}
	
	private static KeyStore loadKeyStore(Config config) throws Exception {		
		try(FileInputStream inputStream = new FileInputStream(config.getString("file"))) {
			KeyStore keyStore = KeyStore.getInstance("JKS");
			
			keyStore.load(inputStream, config.getString("password").toCharArray());
			return keyStore;
		}		
	}
	
	@Override
	public void preStart() throws Exception {
		log.debug("ssl init");
		SSLContext sslContext = SSLContext.getInstance("TLS");
		
		log.debug("loading private keys");
		KeyStore privateKeys = loadKeyStore(config.getConfig("private"));
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
		keyManagerFactory.init(privateKeys, config.getString("private.password").toCharArray());
		KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
		
		final TrustManager[] trustManagers;
		if(config.hasPath("trusted")) {
			log.debug("loading trusted keys");
			KeyStore trustedKeys = loadKeyStore(config.getConfig("trusted"));
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
	        trustManagerFactory.init(trustedKeys);
	        trustManagers = trustManagerFactory.getTrustManagers();
		} else {
			log.warning("no trusted keys configured");
			trustManagers = new TrustManager[] {
					new X509TrustManager() {

						@Override
						public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
							
						}

						@Override
						public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
							
						}

						@Override
						public X509Certificate[] getAcceptedIssuers() {
							return new X509Certificate[0];
						}
					}
			};
		}
		
        log.debug("initializing ssl context");
		sslContext.init(keyManagers, trustManagers, null);
		
		log.debug("creating ssl engine");
		sslEngine = sslContext.createSSLEngine();
		sslEngine.setUseClientMode(!isServer);
		sslEngine.setWantClientAuth(false);
		
		SSLSession sslSession = sslEngine.getSession();
		int bufferSize = sslSession.getApplicationBufferSize() + 50;
		
		log.debug("allocating buffers, size: " + bufferSize);
		
		netInput = ByteString.empty();
		netOutput = ByteBuffer.allocate(bufferSize);
		
		appInput = ByteBuffer.allocate(bufferSize);
		appOutput = ByteString.empty();
	}
	
	private void handleHandshake(SSLEngineResult sslResult) {		
		final HandshakeStatus status = sslResult.getHandshakeStatus();
		
		if(status == HandshakeStatus.FINISHED) {
			log.debug("handshake completed: flushing write buffer");
			getSelf().tell(TcpMessage.write(ByteString.empty()), getSelf());
			getContext().become(handshakeCompleted());
		} else {
			log.debug("handshake in progress");
			
			if(status == HandshakeStatus.NEED_TASK) {
				log.debug("executing delegated tasks");
				
				Runnable r = sslEngine.getDelegatedTask();
				while(r != null) {
					r.run();
					r = sslEngine.getDelegatedTask();
				}
			} else {
				log.debug("no delegated tasks pending: " + status);
			}
			
			if(sslEngine.getHandshakeStatus() == HandshakeStatus.NEED_WRAP) {
				log.debug("requesting wrap");
				getSelf().tell(TcpMessage.write(ByteString.empty()), getSelf());
			} else {
				log.debug("no wrap required at the moment: " + sslEngine.getHandshakeStatus());
			}
			
			if(status == HandshakeStatus.NEED_UNWRAP) {
				if(sslResult != null && sslResult.getStatus() != Status.BUFFER_UNDERFLOW) { 
					log.debug("requesting unwrap");
					getSelf().tell(new Received(ByteString.empty()), getSelf());
				} else {
					log.debug("waiting for more input");
				}
			} else {
				log.debug("no unwrap required at the moment: " + sslEngine.getHandshakeStatus());
			}
		}
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Received) {
			final SSLEngineResult sslResult = unwrap(msg);	
			
			handleHandshake(sslResult);			
			sendAppData(sslResult);
		} else if(msg instanceof Tcp.Write) {
			final SSLEngineResult sslResult = wrap(msg);
			
			handleHandshake(sslResult);
			sendNetData(sslResult);			
		} else if(msg instanceof ConnectionClosed) {
			listener.tell(msg, getSelf());
		} else {			
			unhandled(msg);
		}
	}	
	
	private Procedure<Object> handshakeCompleted() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Received) {			
					sendAppData(unwrap(msg));
				} else if(msg instanceof Tcp.Write) {
					sendNetData(wrap(msg));
				} if(msg instanceof ConnectionClosed) {
					listener.tell(msg, getSelf());
				} else {
					unhandled(msg);
				}
			}
		};
	}
	
	private Procedure<Object> waitingForAck() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					log.debug("write acknowledged");
					
					unstashAll();
					getContext().unbecome();
				} else {
					stash();
				}
			}
			
		};
	}

	private void sendNetData(final SSLEngineResult sslResult) {
		if(sslResult.bytesProduced() > 0) {
			log.debug("sending net data: " + sslResult.bytesProduced());
			
			netOutput.flip();
			connection.tell(TcpMessage.write(ByteString.fromByteBuffer(netOutput), new Ack()), getSelf());
			netOutput.clear();
			
			getContext().become(waitingForAck(), false);
		}
	}

	private SSLEngineResult wrap(Object msg) throws SSLException {
		log.debug("app data received (wrap)");
		
		appOutput = appOutput.concat(((Tcp.Write) msg).data());
		
		SSLEngineResult sslResult = sslEngine.wrap(appOutput.asByteBuffer(), netOutput);
		log.debug("sslResult: " + sslResult);
		
		appOutput = appOutput.drop(sslResult.bytesConsumed());
		log.debug("pending: " + appOutput.size());
		if(appOutput.size() > 0) {
			log.debug("requesting wrap");
			getSelf().tell(TcpMessage.write(ByteString.empty()), getSelf());
		}
		
		return sslResult;		
	}

	private void sendAppData(SSLEngineResult sslResult) {
		if(sslResult.bytesProduced() > 0) {
			log.debug("sending app data: " + sslResult.bytesProduced());
			
			appInput.flip();
			listener.tell(new Received(ByteString.fromByteBuffer(appInput)), getSelf());
			appInput.clear();
		}
	}

	private SSLEngineResult unwrap(Object msg) throws SSLException {
		log.debug("net data received (unwrap)");
		
		netInput = netInput.concat(((Received) msg).data());
		
		SSLEngineResult sslResult = sslEngine.unwrap(netInput.toByteBuffer(), appInput);		
		log.debug("sslResult: " + sslResult);
		
		netInput = netInput.drop(sslResult.bytesConsumed());		
		log.debug("pending: " + netInput.size());
		if(netInput.size() > 0) {
			log.debug("requesting unwrap");
			getSelf().tell(new Received(ByteString.empty()), getSelf());
		}
		
		return sslResult;
	}
}

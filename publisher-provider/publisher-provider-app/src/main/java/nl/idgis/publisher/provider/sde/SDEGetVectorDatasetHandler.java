package nl.idgis.publisher.provider.sde;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.database.DatabaseType;
import nl.idgis.publisher.provider.database.messages.AbstractDatabaseColumnInfo;
import nl.idgis.publisher.provider.database.messages.DatabaseTableInfo;
import nl.idgis.publisher.provider.database.messages.DescribeTable;
import nl.idgis.publisher.provider.database.messages.FetchTable;
import nl.idgis.publisher.provider.protocol.DatasetNotAvailable;
import nl.idgis.publisher.provider.protocol.GetVectorDataset;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.stream.messages.Stop;
import nl.idgis.publisher.stream.messages.Unavailable;
import nl.idgis.publisher.utils.FutureUtils;

import scala.concurrent.duration.Duration;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

public class SDEGetVectorDatasetHandler extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef originalSender;
	
	private final GetVectorDataset originalMsg;
	
	private ActorRef transaction;

	private String tableName;

	private FutureUtils f;

	private SDEUtils sdeUtils;

	private Config databaseConfig;
	
	public SDEGetVectorDatasetHandler(ActorRef originalSender, GetVectorDataset originalMsg, Config databaseConfig) {
		this.originalSender = originalSender;
		this.originalMsg = originalMsg;
		this.databaseConfig = databaseConfig;
		this.sdeUtils = new SDEUtils(databaseConfig);
	}
	
	@Override
	public void preStart() {
		getContext().setReceiveTimeout(Duration.create(30, TimeUnit.SECONDS));
		f = new FutureUtils(getContext());
	}
	
	public static Props props(ActorRef originalSender, GetVectorDataset originalMsg, Config databaseConfig) {
		return Props.create(SDEGetVectorDatasetHandler.class, originalSender, originalMsg, databaseConfig);
	}
	
	private void sendUnavailableAndStop() {
		originalSender.tell(new DatasetNotAvailable(originalMsg.getIdentification()), originalSender);
		getContext().stop(getSelf());
	}
	
	private void unavailable() {
		log.error("dataset unavailable");
		
		if(transaction == null) {
			sendUnavailableAndStop();
		} else {
			f.ask(transaction, new Commit()).thenRun(this::sendUnavailableAndStop);
		}
	}
	
	private Procedure<Object> onReceiveCommitAck() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					log.debug("transaction finished");
					getContext().stop(getSelf());
				} else if(msg instanceof ReceiveTimeout) {
					log.error("timeout received");
					getContext().stop(getSelf());
				}
			}
		};
	}
	
	private Procedure<Object> onReceiveItems() {
		return new Procedure<Object>() {
			
			ActorRef consumer = originalSender;
			
			ActorRef producer;

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Item) {
					log.debug("item");
					producer = getSender();
					consumer.tell(msg, getSelf());
				} else if(msg instanceof NextItem) {
					log.debug("next");
					consumer = getSender();
					producer.tell(msg, getSelf());
				} else if(msg instanceof Stop) {
					log.warning("stop");
					producer.tell(msg, getSender());
				} else if(msg instanceof End) {
					log.debug("end");
					consumer.tell(msg, getSelf());
					transaction.tell(new Commit(), getSelf());
					getContext().become(onReceiveCommitAck());
				} else if(msg instanceof ReceiveTimeout) {
					log.error("timeout received");
					consumer.tell(new Unavailable(), getSelf());
					transaction.tell(new Commit(), getSelf());
					getContext().become(onReceiveCommitAck());
				} else {
					unhandled(msg);
				}
			}
			
		};
	}
	
	private Procedure<Object> onReceiveDatabaseTableInfo() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof DatabaseTableInfo) {
					Map<String, AbstractDatabaseColumnInfo> columnInfos =
						Stream.of(((DatabaseTableInfo) msg).getColumns())
							.collect(Collectors.toMap(
								columnInfo -> columnInfo.getName(),
								columnInfo -> columnInfo));
					
					List<String> columnNames = originalMsg.getColumnNames();
					if(columnNames.stream().allMatch(columnInfos::containsKey)) {
						log.debug("fetching records");
						transaction.tell(
							new FetchTable(
								tableName, 
								columnNames.stream()
									.map(columnInfos::get)
									.collect(Collectors.toList()),
								originalMsg.getMessageSize()),
							getSelf());
						
						getContext().become(onReceiveItems());
					} else {
						log.error("missing column(s)");
						unavailable();
					}
				} else if(msg instanceof ReceiveTimeout) {
					log.debug("timeout received");
					unavailable();
				} else {
					unhandled(msg);
				}
			}
			
		};
	}
	
	
	private Procedure<Object> onReceiveItemRecords() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof SDEItemInfo) {
					log.debug("item info received");
					
					SDEItemInfo itemInfo = (SDEItemInfo)msg;
					SDEItemInfoType type = itemInfo.getType();

					DatabaseType databaseVendor;

					if (databaseConfig.hasPath("vendor")) {
						try {
							databaseVendor = DatabaseType.valueOf(databaseConfig.getString("vendor").toUpperCase());
						} catch(IllegalArgumentException iae) {
							throw new ConfigException.BadValue("vendor", "Invalid vendor supplied in config");
						}
					} else {
						databaseVendor = DatabaseType.ORACLE;
					}

					String tableName = databaseVendor == DatabaseType.POSTGRES ? itemInfo.getPhysicalname().toLowerCase() : itemInfo.getPhysicalname();

					if(SDEItemInfoType.FEATURE_CLASS == type ||
						SDEItemInfoType.TABLE == type) {
						log.debug("tableName: {}", tableName);
						
						SDEGetVectorDatasetHandler.this.tableName = tableName;
						transaction.tell(new DescribeTable(tableName), getSelf());
						getContext().become(onReceiveDatabaseTableInfo());
					} else {
						log.error("wrong item type: {}", type);
						unavailable();
					}
				} else if(msg instanceof ReceiveTimeout) {
					log.debug("timeout received");
					unavailable();
				}
			}
			
		};
	}
	

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof TransactionCreated) {
			log.debug("transaction created");
			
			transaction = ((TransactionCreated)msg).getActor();
			ActorRef recordsReceiver = getContext().actorOf(
				SDEReceiveSingleItemInfo.props(getSelf()), 
				"item-records-receiver");

			String databaseScheme;
			try {
				databaseScheme = databaseConfig.getString("scheme");
			} catch(ConfigException.Missing cem) {
				databaseScheme = "SDE";
			}
			
			log.debug("database scheme before calling get fetch table: " + databaseScheme);
			
			transaction.tell(
				sdeUtils.getFetchTable(sdeUtils.getItemsFilter(originalMsg.getIdentification()), databaseScheme),
				recordsReceiver);
			getContext().become(onReceiveItemRecords());
		} else if(msg instanceof ReceiveTimeout) {
			log.debug("timeout received", msg);
			unavailable();
		} else {
			unhandled(msg);
		}
	}
}

package nl.idgis.publisher.admin;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import nl.idgis.publisher.domain.Failure;
import nl.idgis.publisher.domain.query.DeleteEntity;
import nl.idgis.publisher.domain.query.GetEntity;
import nl.idgis.publisher.domain.query.GetGroupStructure;
import nl.idgis.publisher.domain.query.ListEntity;
import nl.idgis.publisher.domain.query.PutEntity;
import nl.idgis.publisher.domain.query.RefreshDataset;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.DataSource;
import nl.idgis.publisher.domain.web.Dataset;
import nl.idgis.publisher.domain.web.NotFound;
import nl.idgis.publisher.domain.web.SourceDataset;

import nl.idgis.publisher.recorder.AnyRecorder;
import nl.idgis.publisher.recorder.Recording;
import nl.idgis.publisher.recorder.messages.Clear;
import nl.idgis.publisher.recorder.messages.Cleared;
import nl.idgis.publisher.recorder.messages.GetRecording;
import nl.idgis.publisher.recorder.messages.Wait;
import nl.idgis.publisher.recorder.messages.Waited;
import nl.idgis.publisher.utils.SyncAskHelper;

public class AdminTest {
	
	static class On extends AbstractAdmin {
		
		private final ActorRef recorder;
		
		public On(ActorRef database, ActorRef recorder) {
			super(database);
			
			this.recorder = recorder;
		}
		
		public static Props props(ActorRef database, ActorRef recorder) {
			return Props.create(On.class, database, recorder);
		}

		@Override
		protected void preStartAdmin() {
			onDelete(DataSource.class, () -> recorder.tell("delete", getSelf()));
			onPut(Category.class, (category, categoryId) -> recorder.tell(category, getSelf()));			
			onDelete(Category.class, categoryId -> {
				recorder.tell(categoryId, getSelf());
				
				return f.successful(new Category(categoryId, "category-name"));
			}, (category, responseValue) -> recorder.tell(category, getSelf()));
			onDelete(Dataset.class, datasetId -> {
				recorder.tell(datasetId, getSelf());
				
				return f.successful(datasetId);
			}, (datasetId, responseValue) -> recorder.tell(datasetId, getSelf()));
			onQuery(RefreshDataset.class, refreshDataset -> recorder.tell(refreshDataset, getSelf()));			
		}
	}
	
	static class Do extends AbstractAdmin {
		
		public Do(ActorRef database) {
			super(database);
		}
		
		public static Props props(ActorRef database) {
			return Props.create(Do.class, database);
		}
		
		@Override
		@SuppressWarnings("unused")
		protected void preStartAdmin() {
			doDelete(DataSource.class,  dataSourceId -> 
				f.successful(new Response<>(CrudOperation.DELETE, CrudResponse.OK, dataSourceId)));
			doGet(SourceDataset.class, sourceDatasetId -> f.successful(null));
			doGet(Dataset.class, datasetId -> {
				Objects.requireNonNull(null); // raise NPE
				
				return f.successful(null); // unreachable
			});
			doGet(DataSource.class, dataSourceId -> 
				f.failed(new IllegalArgumentException(dataSourceId)));
			doGet(Category.class, categoryId ->
				f.successful(Optional.of(new Category(categoryId, "name: " + categoryId))));
			doList(Category.class, page -> {
				Page.Builder<Category> builder = new Page.Builder<>();
				
				for(int i = 0; i < 10; i++) {
					builder.add(new Category("id" + i, "name: " + i));
				}
				
				return f.successful(builder.build());
			});
			doPut(Category.class, category ->
				f.successful(new Response<>(CrudOperation.CREATE, CrudResponse.OK, category.id())));			
			doDelete(Category.class, categoryId ->
				f.successful(new Response<>(CrudOperation.DELETE, CrudResponse.OK, categoryId)));			
			doDelete(Dataset.class, datasetId ->
				f.successful(new Response<>(CrudOperation.DELETE, CrudResponse.NOK, datasetId)));
			doQuery(RefreshDataset.class, refreshDataset -> f.successful(true));
			doQueryOptional(GetGroupStructure.class, getGroupStructure -> f.successful(Optional.empty()));
		}
		
	}
	
	static class Parent extends AbstractAdminParent {
		
		ActorRef database, recorder;
		
		public Parent(ActorRef database, ActorRef recorder) {
			this.database = database;
			this.recorder = recorder;
		}
		
		public static Props props(ActorRef database, ActorRef recorder) {
			return Props.create(Parent.class, database, recorder);
		}

		@Override
		protected void createActors() {
			createAdminActor(Do.props(database), "do");
			createAdminActor(On.props(database, recorder), "on");
		}
	}
	
	ActorRef recorder, parent;
	
	SyncAskHelper sync;
	
	@Before
	public void actorSystem() throws Exception {
		Config akkaConfig = ConfigFactory.empty()
			.withValue("akka.loggers", ConfigValueFactory.fromIterable(Arrays.asList("akka.event.slf4j.Slf4jLogger")))
			.withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("DEBUG"));
		
		ActorSystem actorSystem = ActorSystem.create("test", akkaConfig);
		
		recorder = actorSystem.actorOf(AnyRecorder.props(), "recorder");		
		parent = actorSystem.actorOf(Parent.props(null, recorder), "parent");
		
		sync = new SyncAskHelper(actorSystem);
	}
	
	@Test
	public void testGet() throws Exception {
		sync.ask(parent, new GetEntity<>(Category.class, "testCategory"), Category.class);
	}
	
	@Test
	public void testList() throws Exception {
		sync.ask(parent, new ListEntity<>(Category.class, 0), Page.class);
	}
	
	@Test
	public void testPut() throws Exception {
		sync.ask(parent, new PutEntity<>(new Category("id", "name")), Response.class);
		sync.ask(recorder, new Wait(1), Waited.class);
		sync.ask(recorder, new GetRecording(), Recording.class)
			.assertNext(Category.class, category -> {
				assertEquals("id", category.id());
				assertEquals("name", category.name());
			})
			.assertNotHasNext();
	}
	
	@Test
	public void testDeleteOk() throws Exception {		
		sync.ask(parent, new DeleteEntity<>(Category.class, "categoryId"), Response.class);
		sync.ask(recorder, new Wait(2), Waited.class);
		sync.ask(recorder, new GetRecording(), Recording.class)
			.assertNext(String.class, categoryId -> {
				assertEquals("categoryId", categoryId);
			})
			.assertNext(Category.class, category -> {
				assertEquals("categoryId", category.id());
				assertEquals("category-name", category.name());
			})
			.assertNotHasNext();
		sync.ask(recorder, new Clear(), Cleared.class);		
		
		sync.ask(parent, new DeleteEntity<>(DataSource.class, "dataSourceId"), Response.class);
		sync.ask(recorder, new Wait(1), Waited.class);
		sync.ask(recorder, new GetRecording(), Recording.class)
			.assertNext(String.class, categoryId -> {
				assertEquals("delete", categoryId);
			})
			.assertNotHasNext();
		sync.ask(recorder, new Clear(), Cleared.class);
	}
	
	@Test
	public void testDeleteNok() throws Exception {
		sync.ask(parent, new DeleteEntity<>(Dataset.class, "datasetId"), Response.class);
		sync.ask(parent, new DeleteEntity<>(Category.class, "categoryId"), Response.class);
		sync.ask(recorder, new Wait(3), Waited.class);
		sync.ask(recorder, new GetRecording(), Recording.class)
			.assertNext(String.class, datasetId -> {
				assertEquals("datasetId", datasetId);
			})
			.assertNext(String.class, categoryId -> {
				assertEquals("categoryId", categoryId);
			})
			.assertNext(Category.class, category -> {
				assertEquals("categoryId", category.id());
				assertEquals("category-name", category.name());
			})
			.assertNotHasNext();
		sync.ask(recorder, new Clear(), Cleared.class);
	}
	
	@Test
	public void testQuery() throws Exception {
		sync.ask(parent, new RefreshDataset("datasetId"));
		sync.ask(recorder, new Wait(1), Waited.class);
		sync.ask(recorder, new GetRecording(), Recording.class)
			.assertNext(RefreshDataset.class, refreshDataset -> {
				assertEquals("datasetId", refreshDataset.getDatasetId());
			})
			.assertNotHasNext();
	}
	
	@Test
	public void testOptionalQuery() throws Exception {
		sync.ask(parent, new GetGroupStructure("groupId"), NotFound.class);
	}
	
	@Test
	public void testCompletedExceptionally() throws Exception {
		sync.ask(parent, new GetEntity<>(DataSource.class, "dataSourceId"), Failure.class);
	}
	
	@Test
	public void testException() throws Exception {
		sync.ask(parent, new GetEntity<>(Dataset.class, "datasetId"), Failure.class);
	}
	
	@Test
	public void testCompletedWithNull() throws Exception {
		sync.ask(parent, new GetEntity<>(SourceDataset.class, "sourceDataset"), Failure.class);
	}
}

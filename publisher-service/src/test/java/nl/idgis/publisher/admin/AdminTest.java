package nl.idgis.publisher.admin;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import nl.idgis.publisher.domain.query.DeleteEntity;
import nl.idgis.publisher.domain.query.GetEntity;
import nl.idgis.publisher.domain.query.ListEntity;
import nl.idgis.publisher.domain.query.PutEntity;
import nl.idgis.publisher.domain.query.RefreshDataset;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.Category;

import nl.idgis.publisher.recorder.AnyRecorder;
import nl.idgis.publisher.recorder.Recording;
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
			onPut(Category.class, category -> recorder.tell(category, getSelf()));			
			onDelete(Category.class, categoryId -> recorder.tell(categoryId, getSelf()));
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
		protected void preStartAdmin() {
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
			doQuery(RefreshDataset.class, refreshDataset -> f.successful(true));
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
			getContext().actorOf(Do.props(database), "do");
			getContext().actorOf(On.props(database, recorder), "on");
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
		
		Thread.sleep(100); // registering query handlers takes a while
	}
	
	@Test
	public void testGet() throws Exception {
		sync.ask(parent, new GetEntity<>(Category.class, "testCatagory"), Category.class);
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
	public void testDelete() throws Exception {
		sync.ask(parent, new DeleteEntity<>(Category.class, "categoryId"), Response.class);
		sync.ask(recorder, new Wait(1), Waited.class);
		sync.ask(recorder, new GetRecording(), Recording.class)
			.assertNext(String.class, categoryId -> {
				assertEquals("categoryId", categoryId);
			})
			.assertNotHasNext();
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
}

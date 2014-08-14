package nl.idgis.publisher.domain.job;

import static org.junit.Assert.assertEquals;

import nl.idgis.publisher.domain.job.JobLogTypes;

import org.junit.Test;

public class JobLogTypesTest {

	@Test
	public void testToEvent() {
		assertEquals(HarvestJobLogType.SOURCE_DATASET_REGISTERED, JobLogTypes.toEnum("HARVEST.SOURCE_DATASET_REGISTERED"));
	}
	
	@Test
	public void testToString() {
		assertEquals("HARVEST.SOURCE_DATASET_REGISTERED", JobLogTypes.toString(HarvestJobLogType.SOURCE_DATASET_REGISTERED));
	}
}

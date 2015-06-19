package nl.idgis.publisher.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Optional;

import org.junit.Test;

public class EitherTest {

	@Test
	public void testLeft() {
		Either<String, ?> either = Either.left("Hello, world!");
		
		Optional<String> optional = either.getLeft();
		assertTrue(optional.isPresent());
		assertEquals("Hello, world!", optional.get());
		
		assertFalse(either.getRight().isPresent());
	}
	
	@Test
	public void testRight() {
		Either<?, String> either = Either.right("Hello, world!");
		
		Optional<String> optional = either.getRight();
		assertTrue(optional.isPresent());
		assertEquals("Hello, world!", optional.get());
		
		assertFalse(either.getLeft().isPresent());
	}
	
	@Test
	public void testMapLeft() {
		Either<String, Integer> either = Either.left("Hello, world!");
		assertEquals(Integer.valueOf(42), either.mapLeft(s -> 42));
		assertEquals("Hello, world!", either.mapRight(o -> {
			fail("mapper called");			
			return "Not used";
		}));
	}
	
	@Test
	public void testMapRight() {
		Either<Integer, String> either = Either.right("Hello, world!");
		assertEquals(Integer.valueOf(42), either.mapRight(s -> 42));
		assertEquals("Hello, world!", either.mapLeft(o -> {
			fail("mapper called");			
			return "Not used";
		}));
	}
	
	@Test
	public void testMap() {
		Either<Integer, Double> either = Either.left(42);
		assertEquals("42", either.map(i -> i.toString(), d -> d.toString()));
		
		either = Either.right(42.47);
		assertEquals("42.47", either.map(i -> i.toString(), d -> d.toString()));
	}
	
	@Test
	public void testSwap() {
		Either<?, String> either = Either.left("Hello, world!");
		assertTrue(either.getLeft().isPresent());
		assertTrue(either.swap().getRight().isPresent());
	}
}

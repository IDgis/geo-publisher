package nl.idgis.publisher.utils;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Various {@link Stream} utilities. 
 *
 */
public class StreamUtils {
	
	/**
	 * A zipped entry.
	 */
	public static class ZippedEntry<T, U> {
		
		private final T first;
		
		private final U second;
		
		private ZippedEntry(T first, U second) {
			this.first = first;
			this.second = second;
		}
		
		/**
		 * Returns the first item to this entry.
		 * @return the first item to this entry
		 */
		public T getFirst() {
			return first;
		}
		
		/**
		 * Returns the second item to this entry.
		 * @return the second item to this entry
		 */
		public U getSecond() {
			return second;
		}

		@Override
		public String toString() {
			return "ZippedEntry [first=" + first + ", second=" + second + "]";
		}
	}
	
	/** 
	 * An indexed entry.
	 */
	public static class IndexedEntry<T> {
		
		private final int index;
		
		private final T value;
		
		private IndexedEntry(int index, T value) {
			this.index = index;
			this.value = value;
		}

		/**
		 * Returns the index corresponding to this entry.
		 * @return the index corresponding to this entry
		 */
		public int getIndex() {
			return index;
		}

		/**
		 * Returns the value corresponding to this entry.
		 * @return the value corresponding to this entry
		 */
		public T getValue() {
			return value;
		}
	}
	
	/**
	 * Creates an indexed stream.
	 * 
	 * @param first the stream
	 * @return a new stream containing {@link IndexedEntry} objects
	 */
	public static <T> Stream<IndexedEntry<T>> index(Stream<T> stream) {
		return zip(
			IntStream.rangeClosed(0, Integer.MAX_VALUE).boxed(), 
			stream.limit(Integer.MAX_VALUE), 
			(index, value) -> new IndexedEntry<>(index, value));
	}
	
	/**
	 * Creates a zipped stream.
	 * 
	 * @param first the first stream
	 * @param second the second stream
	 * @return a new stream containing {@link ZippedEntry} objects
	 */
	public static <T, U> Stream<ZippedEntry<T, U>> zip(Stream<T> first, Stream<U> second) {
		return zip(first, second, (t, u) -> new ZippedEntry<>(t, u));
	}
	
	/**
	 * Creates a zipped map.
	 * 
	 * @param first
	 * @param second
	 * @return a map with elements of <code>first</code> as keys 
	 * 			and elements of <code>second</code> as values.
	 */
	public static <T, U> Map<T, U> zipToMap(Stream<T> first, Stream<U> second) {
		return 
			zip(first, second)
				.collect(Collectors.toMap(
					ZippedEntry::getFirst, 
					ZippedEntry::getSecond));
	}

	/**
	 * Creates a zipped stream.
	 * 
	 * @param first the first stream
	 * @param second the second stream
	 * @param zipper the zipper function
	 * @return a new stream containing zipped elements
	 */
	public static <T, U, R> Stream<R> zip(Stream<? extends T> first, Stream<? extends U> second, BiFunction<? super T, ? super U, ? extends R> zipper) {
		Objects.requireNonNull(zipper);
		
		Spliterator<? extends T> firstSplr = first.spliterator();
		Spliterator<? extends U> secondSplr = second.spliterator();
		
		Iterator<R> itr = new Iterator<R>() {
			
			Iterator<? extends T> firstItr = Spliterators.iterator(firstSplr);
			Iterator<? extends U> secondItr = Spliterators.iterator(secondSplr);

			@Override
			public boolean hasNext() {
				return firstItr.hasNext() && secondItr.hasNext();
			}

			@Override
			public R next() {
				return zipper.apply(firstItr.next(), secondItr.next());
			}
		};
		
		int characteristics = 
			firstSplr.characteristics()
			& secondSplr.characteristics()
			& ~(Spliterator.DISTINCT 
				| Spliterator.SORTED
				| Spliterator.NONNULL)
			| Spliterator.IMMUTABLE;
		
		boolean parallel = first.isParallel() || second.isParallel();
		
		long size = Math.min(firstSplr.getExactSizeIfKnown(), secondSplr.getExactSizeIfKnown());
        
        return StreamSupport.stream(
        	size == -1
        		? Spliterators.spliteratorUnknownSize(itr, characteristics)
        		: Spliterators.spliterator(itr, size, characteristics),
        	
        	parallel);
	}
}

package nl.idgis.publisher.utils;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Function;
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
	
	public static class Wrapper<T, U> implements Comparable<Wrapper<T, U>> {
		
		private final T value;
		
		private final Function<? super T, ? extends U> mapper;
		
		private final Comparator<? super U> comparator;
		
		Wrapper(T value, Function<? super T, ? extends U> mapper, Comparator<? super U> comparator) {
			this.value = value;
			this.mapper = mapper;
			this.comparator = comparator;
		}
		
		/**
		 * Unwrap wrapped value.
		 * 
		 * @return the original unwrapped value
		 */
		public T unwrap() {
			return value;
		}

		@Override
		public int compareTo(Wrapper<T, U> o) {
			return comparator.compare(mapper.apply(value), mapper.apply(o.value));
		}

		@Override
		public int hashCode() {
			return mapper.apply(value).hashCode();
		}
		
		@Override
		@SuppressWarnings("unchecked")
		public boolean equals(Object obj) {
			return mapper.apply(value).equals(mapper.apply(((Wrapper<T, U>)obj).value));
		}
	}
	
	/**
	 * Generates a function to wrap comparable values in a {@link Wrapper}.
	 * 
	 * @param mapper the function to generate the value to be wrapped  
	 * @return the wrapper function
	 */
	public static <T, U extends Comparable<? super U>> Function<T, Wrapper<T, U>> wrap(Function<? super T, ? extends U> mapper) {
		return wrap(mapper, Comparator.naturalOrder());
	}
	
	/**
	 * Generates a function to wrap values in a {@link Wrapper}.
	 * 
	 * @param mapper the function to generate the value to be wrapped
	 * @param comparator the comparator used to compare values
	 * @return the wrapper function
	 */
	public static <T, U> Function<T, Wrapper<T, U>> wrap(Function<? super T, ? extends U> mapper, Comparator<? super U> comparator) {
		return value -> 
			new Wrapper<T, U>(value, 
					Objects.requireNonNull(mapper, "mapper must not be null"), 
					Objects.requireNonNull(comparator, "comparator must not be null"));
	}
}

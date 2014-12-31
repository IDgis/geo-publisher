package nl.idgis.publisher.utils;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamUtils {
	
	public static class ZippedEntry<T, U> {
		
		private final T first;
		
		private final U second;
		
		private ZippedEntry(T first, U second) {
			this.first = first;
			this.second = second;
		}
		
		public T getFirst() {
			return first;
		}
		
		public U getSecond() {
			return second;
		}
	}
	
	public static class IndexedEntry<T> {
		
		private final int index;
		
		private final T value;
		
		private IndexedEntry(int index, T value) {
			this.index = index;
			this.value = value;
		}

		public int getIndex() {
			return index;
		}

		public T getValue() {
			return value;
		}
	}
	
	public static <T> Stream<IndexedEntry<T>> index(Stream<T> stream) {
		return zip(
			IntStream.rangeClosed(0, Integer.MAX_VALUE).boxed(), 
			stream.limit(Integer.MAX_VALUE), 
			(index, value) -> new IndexedEntry<>(index, value));
	}
	
	public static <T, U> Stream<ZippedEntry<T, U>> zip(Stream<T> first, Stream<U> second) {
		return zip(first, second, (t, u) -> new ZippedEntry<>(t, u));
	}

	public static <T, U, R> Stream<R> zip(Stream<T> first, Stream<U> second, BiFunction<T, U, R> entryConstructor) {
		return StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(new Iterator<R>() {
					
					Iterator<T> firstItr = first.iterator();
					Iterator<U> secondItr = second.iterator();

					@Override
					public boolean hasNext() {
						return firstItr.hasNext() && secondItr.hasNext();
					}

					@Override
					public R next() {
						return entryConstructor.apply(firstItr.next(), secondItr.next());
					}
					
				}, Spliterator.ORDERED), false);
	}
}

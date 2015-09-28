package nl.idgis.publisher.utils;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
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

		@Override
		public String toString() {
			return "IndexedEntry [index=" + index + ", value=" + value + "]";
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
	
	/**
	 * <p> Performs a merge join on two ordered streams. The streams are not allowed to provide null values and
	 * should supply instances of classes properly implementing {@link Object#equals(Object) Object.equals}. </p>
	 * 
	 * <p> Although it is safe to provide empty streams, such streams are not processed particularly efficiently. </p>
	 *
	 * <p> This method is identical to {@link #mergeJoin(Stream, Stream, BiFunction, Collector, Collector, Combiner) mergeJoin}
	 * except for the type of the combiner. This method uses a {@link BiFunction} instead of a {@link Combiner}, thus providing
	 * no access to current stream values. </p>
	 * 
	 * @param left the left stream.
	 * @param right the right stream.
	 * @param comparator a comparator. It compares its two arguments for order. Returns a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
	 * @param leftCollector the collector used to collect items from the left stream.
	 * @param rightCollector the collector used to collect items from the right stream.
	 * @param combiner the final combining function transforming the collected items into a result item.
	 * @return the resulting stream of items generated by the combiner.
	 */
	public static  <T, U, TV, UV, TA, UA, R> Stream<R> mergeJoin(Stream<T> left, Stream<U> right, BiFunction<? super T, ? super U, Integer> comparator, 
		Collector<? super T, TA, TV> leftCollector, Collector<? super U, UA, UV> rightCollector, BiFunction<? super TV, ? super UV, R> combiner) {
		
		return mergeJoin(left, right, comparator, leftCollector, rightCollector, (leftResult, rightResult, peekLeft, peekRight) -> combiner.apply(leftResult, rightResult));
	}
	
	/**
	 * Combiner function for {@link #mergeJoin(Stream, Stream, BiFunction, Collector, Collector, Combiner) mergeJoin}.
	 *
	 * @param <T>
	 * @param <U>
	 * @param <TV>
	 * @param <UV>
	 * @param <R>
	 */
	@FunctionalInterface
	public interface Combiner<T, U, TV, UV, R> {		
		R apply(TV leftResult, UV rightResult, Optional<T> peekLeft, Optional<U> peekRight);
	}
	
	/**
	 * <p> Performs a merge join on two ordered streams. The streams are not allowed to provide null values and
	 * should supply instances of classes properly implementing {@link Object#equals(Object) Object.equals}. </p>
	 * 
	 * <p> Although it is safe to provide empty streams, such streams are not processed particularly efficiently. </p>
	 * 
	 * <p> This method is identical to {@link #mergeJoin(Stream, Stream, BiFunction, Collector, Collector, BiFunction) mergeJoin}
	 * except for the type of the combiner. This method uses a {@link Combiner} which provides access to current stream values.
	 * This information can be used to obtain knowledge about the content of the streams that is hard to obtain without actually
	 * consuming items from the stream. </p> 
	 * 
	 * @param left the left stream.
	 * @param right the right stream.
	 * @param comparator a comparator. It compares its two arguments for order. Returns a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
	 * @param leftCollector the collector used to collect items from the left stream.
	 * @param rightCollector the collector used to collect items from the right stream.
	 * @param combiner the final combining function transforming the collected items into a result item.
	 * @return the resulting stream of items generated by the combiner.
	 */
	public static <T, U, TV, UV, TA, UA, R> Stream<R> mergeJoin(Stream<T> left, Stream<U> right, BiFunction<? super T, ? super U, Integer> comparator, 
		Collector<? super T, TA, TV> leftCollector, Collector<? super U, UA, UV> rightCollector, Combiner<? super T, ? super U, ? super TV, ? super UV, R> combiner) {
		
		Objects.requireNonNull(left, "left stream must not be null");
		Objects.requireNonNull(right, "right stream must not be null");
		Objects.requireNonNull(comparator, "comparator must not be null");
		Objects.requireNonNull(leftCollector, "left collector must not be null");
		Objects.requireNonNull(rightCollector, "right collector must not be null");
		Objects.requireNonNull(combiner, "combiner must not be null");
		
		// obtain suppliers.
		Supplier<TA> leftSupplier = leftCollector.supplier();
		Supplier<UA> rightSupplier = rightCollector.supplier();
		
		// obtain accumulators.
		BiConsumer<TA, ? super T> leftAccumulator = leftCollector.accumulator();		
		BiConsumer<UA, ? super U> rightAccumulator = rightCollector.accumulator();
		
		// obtain finishers.
		Function<TA, TV> leftFinisher = leftCollector.finisher();
		Function<UA, UV> rightFinisher = rightCollector.finisher();
		
		// obtain iterators.
		Iterator<T> leftItr = left.iterator();		
		Iterator<U> rightItr = right.iterator();
		
		// create an iterators that obtains items from both streams. 
		Iterator<R> itr = new Iterator<R>() {
			
			// current, previous and next items from left stream.
			T leftItem, prevLeftItem, peekLeft;
			
			// current, previous items from left stream.
			U rightItem, prevRightItem;
			
			// container for current left item (provided by left collector).
			TA leftContainer;
			
			// container for current right item (provided by right collector).
			UA rightContainer;
			
			// indicates whether one of the containers is applied to
			// its accumulator (such a container is assumed to be not empty anymore).
			boolean isEmpty;

			@Override
			public boolean hasNext() {
				return leftItr.hasNext() || rightItr.hasNext();
			}
			
			/**
			 *  provide a new item based on the current state of the iterator.
			 * @return
			 */
			R finish() {
				if(isEmpty) { // omit item if both containers are empty.
					return next();
				}
				
				// obtain item by calling both finishers and the combiner.
				return combiner.apply(
					leftFinisher.apply(leftContainer),
					rightFinisher.apply(rightContainer),
					Optional.ofNullable(leftItem),
					Optional.ofNullable(rightItem));
			}
			
			/**
			 * add current left item to container
			 */
			void addLeft() {
				if(!leftItem.equals(prevLeftItem)) {
					leftAccumulator.accept(leftContainer, leftItem);
					prevLeftItem = leftItem;
					isEmpty = false;
				}
			}
			
			/**
			 * add current right item to container.
			 */
			void addRight() {
				if(!rightItem.equals(prevRightItem)) {
					rightAccumulator.accept(rightContainer, rightItem);
					prevRightItem = rightItem;
					isEmpty = false;
				}
			}
			
			/**
			 * obtain next item from left stream (peekLeft aware).
			 */
			void nextLeft() {
				if(peekLeft == null) {
					leftItem = Objects.requireNonNull(leftItr.next(), "left stream provided null value");
				} else {
					leftItem = peekLeft;
					peekLeft = null;
				}
			}
			
			/**
			 * obtain next item from right stream.
			 */
			void nextRight() {
				rightItem = Objects.requireNonNull(rightItr.next(), "right stream provided null value");
			}
			
			/**
			 * peek into the left stream.
			 */
			T peekLeft() {
				if(peekLeft == null) {
					peekLeft = Objects.requireNonNull(leftItr.next(), "left stream provided null value");
				}
				
				return peekLeft;
			}
			
			/**
			 * next item available in right stream?
			 * @return
			 */
			boolean hasNextRight() {
				return rightItr.hasNext();
			}
			
			/**
			 * next item available in left stream? Method is peekLeft aware.
			 * @return
			 */
			boolean hasNextLeft() {
				return peekLeft != null || leftItr.hasNext();
			}
			
			/**
			 * clear containers
			 */
			void clear() {
				isEmpty = true;
				
				leftContainer = leftSupplier.get();
				rightContainer = rightSupplier.get();
			}

			@Override
			public R next() {
				clear();				

				// init left.
				if(leftItem == null && hasNextLeft()) {
					nextLeft();
				}
				
				// init right.
				if(rightItem == null && hasNextRight()) {
					nextRight();
				}

				// we can be sure that at this point: lightItem != null || rightItem != null
				// because next() is only called (by the enclosing spliterator) when hasNext() returned true.
				
				// right stream is empty or left iterator is trailing right iterator.
				if(rightItem == null || comparator.apply(leftItem, rightItem) < 0 && hasNextLeft()) {					
					addLeft();
					nextLeft();
					
					return finish();
				}
				
				// left stream is empty or right iterator is trailing left iterator.
				if(leftItem == null || comparator.apply(leftItem, rightItem) > 0 && hasNextRight()) {
					addRight();						
					nextRight();
					
					return finish();
				}
				
				// left and right are equal.
				while(comparator.apply(leftItem, rightItem) == 0) {
					addLeft();
					addRight();
					
					if(hasNextRight()) {
						// peek into left stream to avoid advancing right iterator prematurely.
						while(hasNextLeft() && comparator.apply(peekLeft(), rightItem) == 0) {
							nextLeft();
							addLeft();
						}
												
						nextRight();
					} else {
						if(hasNextLeft()) {
							nextLeft();
						} else {							
							break;
						}
					}
				}
				
				return finish();
			}
			
		};
		
		// no spliterator characteristics are set because 
		// we can't guarantee anything regarding stream content.
		return toStream(itr, 0, false);
	}

	private static <R> Stream<R> toStream(Iterator<R> itr, int characteristics, boolean parallel) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(itr, characteristics), parallel);
	}
	
	public static class Partition<K, D extends Collection<E>, E> {
		
		private final K key;
		
		private final D container;
		
		private Partition(K key, D container) {
			this.key = key;
			this.container = container;
		}
		
		public K key() {
			return key;
		}
		
		public E first() {
			// container is never empty
			return container.stream().findFirst().get();
		}
		
		public Stream<E> stream() {
			return container.stream();
		}
	}
	
	/**
	 * <p> Partitions a stream with given classifier. This method expects the
	 * stream to be ordered on the same key as the one provided by the classifier. </p>
	 * 
	 * <p> Shorthand for: {@link #partition(Stream, Function, Collector) partition(stream, function, Collectors.toList())} </p>
	 * 
	 * @param stream the stream to partition.
	 * @param classifier the classifier to partition.
	 * @return the partitioned stream.
	 */
	public static <T, K extends Comparable<? super K>> Stream<Partition<K, List<T>, T>> partition(Stream<T> stream, Function<? super T, K> classifier) {
		return partition(stream, classifier, Collectors.toList());
	}
	
	/**
	 * <p> Partitions a stream with given classifier. The provided collector
	 * is used to collect the content of a single partition. This method expects the
	 * stream to be ordered on the same key as the one provided by the classifier. </p>
	 * 
	 * <p> The resulting stream has the same content as the one obtained using the following operation:
	 * <pre>
	 * <code>
	 * stream.collect(
	 *   Collectors.groupingBy(classifier, LinkedHashMap::new, collector)).entrySet().stream()	   
	 *	   .map(entry -> new Partition<>(entry.getKey(), entry.getValue())); 
	 * </code>
	 * </pre> </p>
	 * 
	 * <p> However, this method consumes only a single partition at a time. </p>
	 * 
	 * @param stream the stream to partition.
	 * @param classifier the classifier to partition.
	 * @param collector the collector to collect partition content with. 
	 * @return the partitioned stream.
	 */
	public static <T, K extends Comparable<? super K>, A, D extends Collection<T>> Stream<Partition<K, D, T>> partition(Stream<T> stream, Function<? super T, K> classifier, Collector<T, A, D> collector) {		
		Objects.requireNonNull(stream, "stream must not be null");
		Objects.requireNonNull(classifier, "classifier must not be null");
		Objects.requireNonNull(collector, "collector must not be null");
		
		return toStream(new Iterator<Partition<K, D, T>>() {
		
			T currentItem; // already fetched (but unprocessed) item
			K currentKey, lastKey; // key of current item and of previous item
			Iterator<T> itr = stream.iterator(); // upstream iterator
			
			// cached collector functions
			Supplier<A> supplier = collector.supplier();
			BiConsumer<A, T> accumulator = collector.accumulator();
			Function<A, D> finisher = collector.finisher();

			@Override
			public boolean hasNext() {
				return currentItem != null // pending item 
					|| itr.hasNext();
			}

			@Override
			public Partition<K, D, T> next() {
				// prepare empty container for partition
				A container = supplier.get();
				
				// consume pending item (if any)
				if(currentItem != null) {
					accumulator.accept(container, currentItem);
					currentItem = null;
					lastKey = currentKey;
				}
				
				// consume upstream iterator until we encounter another key
				while(itr.hasNext()) {
					currentItem = itr.next();
					currentKey = classifier.apply(currentItem);
					
					if(lastKey == null || lastKey.equals(currentKey)) {
						// consume item
						accumulator.accept(container, currentItem);
						currentItem = null;
						lastKey = currentKey;
					} else {
						// key change detected -> stop collecting items
						break;
					}
				}
				
				// return current partition
				return new Partition<>(lastKey, finisher.apply(container));
			}
			
		}, 0, false);
	}
}

package nl.idgis.publisher.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mysema.query.Tuple;
import com.mysema.query.types.Expression;

import nl.idgis.publisher.utils.StreamUtils;

public class DatabaseUtils {

	/**
	 * Consume items from the provided {@link ListIterator} while given id matches the
	 * result of the given id {@link Expression}.
	 * 
	 * @param listIterator
	 * @param id
	 * @param idExpression
	 * @param mapper
	 * @return
	 */
	public static <T, U extends Comparable<U>> List<T> consumeList(ListIterator<Tuple> listIterator, U id, Expression<U> idExpression, Function<Tuple, T> mapper) {
		List<T> retval = new ArrayList<>();
		
		U lastListId = null;
		for(; listIterator.hasNext();) {
			Tuple tc = listIterator.next();
			
			U listId = tc.get(idExpression);
			if(lastListId != null && listId.compareTo(lastListId) < 0) {
				throw new IllegalArgumentException("listIterator is not ordered by expression");
			} else {			
				lastListId = listId;
			}
			
			int cmp = listId.compareTo(id);			
			if(cmp > 0) {
				listIterator.previous();
				break;
			} else if(cmp < 0) {
				continue;
			} else {			
				retval.add(mapper.apply(tc));
			}
		}
		
		return retval;
	}
	
	@FunctionalInterface
	private interface JoinFunction<T extends Comparable<? super T>> {
		@SuppressWarnings("unchecked")
		 Stream<Tuple> join(Stream<Tuple> left, Stream<Tuple> right, Expression<? extends T> firstExpr, Expression<? extends T>... otherExprs);
	}
	
	/**
	 * Executes a join function with streams obtains from provided collections.
	 * 
	 * This methods implements logic for dealing with empty collections efficiently. 
	 * 
	 * @param joinFunction the join function to execute.
	 * @param left left collection.
	 * @param right right collection.
	 * @param firstExpr first join condition.
	 * @param otherExprs additional join conditions.
	 * @return the join result.
	 */
	@SafeVarargs
	private static <T extends Comparable<? super T>> Stream<Tuple> join(JoinFunction<T> joinFunction, Collection<Tuple> left, Collection<Tuple> right, Expression<? extends T> firstExpr, Expression<? extends T>... otherExprs) {
		if(left.isEmpty()) {
			if(right.isEmpty()) {
				return Stream.empty();
			} else {
				return right.stream();
			}
		} else {
			if(right.isEmpty()) {
				return left.stream();
			} else {
				return joinFunction.join(left.stream(), right.stream(), firstExpr, otherExprs);
			}
		}
	}
	
	/**
	 * Performs an inner join on provided collections. 
	 * 
	 * This methods implements logic for dealing with empty collections efficiently and its
	 * usage is therefore preferred over using {@link #join(Stream, Stream, Expression, Expression...)} on
	 * streams obtained from a {@link Collection}. 
	 * 
	 * @param left left collection.
	 * @param right right collection.
	 * @param firstExpr first join condition.
	 * @param otherExprs additional join conditions.
	 * @return the join result.
	 */
	@SafeVarargs
	public static <T extends Comparable<? super T>> Stream<Tuple> join(Collection<Tuple> left, Collection<Tuple> right, Expression<? extends T> firstExpr, Expression<? extends T>... otherExprs) {
		return join(DatabaseUtils::join, left, right, firstExpr, otherExprs);
	}
	
	/**
	 * Performs an inner join on provided streams. 
	 * 
	 * Consider using {@link #join(Collection, Collection, Expression, Expression...)} for streams
	 * obtained from a {@link Collection}.  
	 * 
	 * @param left left stream.
	 * @param right right stream.
	 * @param firstExpr first join condition.
	 * @param otherExprs additional join conditions.
	 * @return the join result.
	 */
	@SafeVarargs
	public static <T extends Comparable<? super T>> Stream<Tuple> join(Stream<Tuple> left, Stream<Tuple> right, Expression<? extends T> firstExpr, Expression<? extends T>... otherExprs) {
		return StreamUtils
			.mergeJoin(
				left, 
				right, 
				comparing(firstExpr, otherExprs)::compare,
				Collectors.toList(),
				Collectors.toList(),
				(leftResult, rightResult) -> 
					// generate a JoinedTuple for every combination of leftResult and rightResult.
					leftResult.stream()
						.<Tuple>flatMap(leftItem ->
							rightResult.stream()
								.<Tuple>map(rightItem -> (Tuple)new JoinedTuple(leftItem, rightItem))))
			.flatMap(Function.identity()); // convert Stream<Stream<Tuple>> into Stream<Tuple>.
	}

	/**
	 * Performs a left outer join on provided streams. 
	 * 
	 * This methods implements logic for dealing with empty collections efficiently and its
	 * usage is therefore preferred over using {@link #leftJoin(Stream, Stream, Expression, Expression...)} on
	 * streams obtained from a {@link Collection}. 
	 * 
	 * @param left left collection.
	 * @param right right collection.
	 * @param firstExpr first join condition.
	 * @param otherExprs additional join conditions.
	 * @return the join result.
	 */
	@SafeVarargs
	public static <T extends Comparable<? super T>> Stream<Tuple> leftJoin(Collection<Tuple> left, Collection<Tuple> right, Expression<? extends T> firstExpr, Expression<? extends T>... otherExprs) {
		return join(DatabaseUtils::leftJoin, left, right, firstExpr, otherExprs);
	}
	
	/**
	 * Performs a left outer join on provided streams. 
	 * 
	 * Consider using {@link #leftJoin(Collection, Collection, Expression, Expression...)} for streams
	 * obtained from a {@link Collection}.  
	 * 
	 * @param left left stream.
	 * @param right right stream.
	 * @param firstExpr first join condition.
	 * @param otherExprs additional join conditions.
	 * @return the join result.
	 */
	@SafeVarargs
	public static <T extends Comparable<? super T>> Stream<Tuple> leftJoin(Stream<Tuple> left, Stream<Tuple> right, Expression<? extends T> firstExpr, Expression<? extends T>... otherExprs) {
		return StreamUtils
			.mergeJoin(
				left, 
				right, 
				comparing(firstExpr, otherExprs)::compare,
				Collectors.toList(),
				Collectors.toList(),
				(leftResult, rightResult, peekLeft, peekRight) ->
					// generate a JoinedTuple for every combination of leftResult and rightResult
					// and a JoinedTuple with an EmptyTuple for a every lone leftItem.
					rightResult.isEmpty()
						? leftResult.stream()
							.<Tuple>map(leftItem -> 
								// right stream could be empty in which
								// case we don't know the size of a right tuple.
								peekRight.isPresent()
									? (Tuple)new JoinedTuple(leftItem, new EmptyTuple(peekRight.get().size()))
									: leftItem)
						: leftResult.stream()
							.<Tuple>flatMap(leftItem ->
								rightResult.stream()
									.<Tuple>map(rightItem -> (Tuple)new JoinedTuple(leftItem, rightItem))))
			.flatMap(Function.identity()); // convert Stream<Stream<Tuple>> into Stream<Tuple>.
	}
	
	/**
	 * Creates a {@link Comparator} for comparing {@link Tuple} instances based on given expressions.
	 * 
	 * @param firstExpr first expression.
	 * @param otherExprs additional expressions.
	 * @return the comparator.
	 */
	@SafeVarargs
	public static <T extends Comparable<? super T>> Comparator<Tuple> comparing(Expression<? extends T> firstExpr, Expression<? extends T>... otherExprs) {
		Comparator<Tuple> c = Comparator.comparing(t -> t.get(firstExpr));
		
		for(Expression<? extends T> otherExpr : otherExprs) {
			c = c.thenComparing(Comparator.comparing(t -> t.get(otherExpr)));
		}	
		
		return c;
	}
}

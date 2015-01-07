package nl.idgis.publisher.database;

import java.util.Arrays;
import java.util.List;

import com.infradna.tool.bridge_method_injector.WithBridgeMethods;
import com.mysema.query.JoinFlag;
import com.mysema.query.QueryFlag;
import com.mysema.query.QueryFlag.Position;
import com.mysema.query.QueryMetadata;
import com.mysema.query.QueryModifiers;
import com.mysema.query.sql.ForeignKey;
import com.mysema.query.sql.RelationalFunctionCall;
import com.mysema.query.sql.RelationalPath;
import com.mysema.query.sql.SQLCommonQuery;
import com.mysema.query.sql.SQLOps;
import com.mysema.query.sql.SQLTemplates;
import com.mysema.query.sql.WithBuilder;
import com.mysema.query.support.Expressions;
import com.mysema.query.support.QueryMixin;
import com.mysema.query.types.CollectionExpression;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.Expression;
import com.mysema.query.types.ExpressionUtils;
import com.mysema.query.types.OperationImpl;
import com.mysema.query.types.Operator;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.ParamExpression;
import com.mysema.query.types.Path;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.SubQueryExpression;
import com.mysema.query.types.TemplateExpressionImpl;
import com.mysema.query.types.expr.CollectionExpressionBase;
import com.mysema.query.types.expr.CollectionOperation;
import com.mysema.query.types.query.ListSubQuery;

public abstract class AbstractAsyncSQLQuery<Q extends AbstractAsyncSQLQuery<Q>> implements SQLCommonQuery<Q> {
	
	protected final QueryMixin<Q> queryMixin;

    @SuppressWarnings("unchecked")
    public AbstractAsyncSQLQuery(QueryMetadata metadata) {
    	queryMixin = new QueryMixin<Q>((Q)this, metadata);                
    }
    
    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public Q addFlag(Position position, String prefix, Expression<?> expr) {
        Expression<?> flag = TemplateExpressionImpl.create(expr.getType(), prefix + "{0}", expr);
        return queryMixin.addFlag(new QueryFlag(position, flag));
    }
   
    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public Q addFlag(Position position, String flag) {
        return queryMixin.addFlag(new QueryFlag(position, flag));
    }
    
    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public Q addFlag(Position position, Expression<?> flag) {
        return queryMixin.addFlag(new QueryFlag(position, flag));
    }
   
    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public Q addJoinFlag(String flag) {
        return addJoinFlag(flag, JoinFlag.Position.BEFORE_TARGET);
    }
    
    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    @SuppressWarnings("unchecked")
    public Q addJoinFlag(String flag, JoinFlag.Position position) {
        queryMixin.addJoinFlag(new JoinFlag(flag, position));
        return (Q)this;
    }   

    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public Q from(Expression<?> arg) {
        return queryMixin.from(arg);
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public Q from(Expression<?>... args) {
        return queryMixin.from(args);
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Q from(SubQueryExpression<?> subQuery, Path<?> alias) {
        queryMixin.from(ExpressionUtils.as((Expression)subQuery, alias));
        return (Q)this;
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public Q fullJoin(EntityPath<?> target) {
        return queryMixin.fullJoin(target);
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public <E> Q fullJoin(RelationalFunctionCall<E> target, Path<E> alias) {
        return queryMixin.fullJoin(target, alias);
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public <E> Q fullJoin(ForeignKey<E> key, RelationalPath<E> entity) {
        return queryMixin.fullJoin(entity).on(key.on(entity));
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public Q fullJoin(SubQueryExpression<?> target, Path<?> alias) {
        return queryMixin.fullJoin(target, alias);
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public Q innerJoin(EntityPath<?> target) {
        return queryMixin.innerJoin(target);
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public <E> Q innerJoin(RelationalFunctionCall<E> target, Path<E> alias) {
        return queryMixin.innerJoin(target, alias);
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public <E> Q innerJoin(ForeignKey<E> key, RelationalPath<E> entity) {
        return queryMixin.innerJoin(entity).on(key.on(entity));
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public Q innerJoin(SubQueryExpression<?> target, Path<?> alias) {
        return queryMixin.innerJoin(target, alias);
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public Q join(EntityPath<?> target) {
        return queryMixin.join(target);
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public <E> Q join(RelationalFunctionCall<E> target, Path<E> alias) {
        return queryMixin.join(target, alias);
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public <E> Q join(ForeignKey<E> key, RelationalPath<E>  entity) {
        return queryMixin.join(entity).on(key.on(entity));
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public Q join(SubQueryExpression<?> target, Path<?> alias) {
        return queryMixin.join(target, alias);
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public Q leftJoin(EntityPath<?> target) {
        return queryMixin.leftJoin(target);
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public <E> Q leftJoin(RelationalFunctionCall<E> target, Path<E> alias) {
        return queryMixin.leftJoin(target, alias);
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public <E> Q leftJoin(ForeignKey<E> key, RelationalPath<E>  entity) {
        return queryMixin.leftJoin(entity).on(key.on(entity));
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public Q leftJoin(SubQueryExpression<?> target, Path<?> alias) {
        return queryMixin.leftJoin(target, alias);
    }

    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public Q on(Predicate condition) {
        return queryMixin.on(condition);
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public Q on(Predicate... conditions) {
        return queryMixin.on(conditions);
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public Q rightJoin(EntityPath<?> target) {
        return queryMixin.rightJoin(target);
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public <E> Q rightJoin(RelationalFunctionCall<E> target, Path<E> alias) {
        return queryMixin.fullJoin(target, alias);
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public <E> Q rightJoin(ForeignKey<E> key, RelationalPath<E>  entity) {
        return queryMixin.rightJoin(entity).on(key.on(entity));
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public Q rightJoin(SubQueryExpression<?> target, Path<?> alias) {
        return queryMixin.rightJoin(target, alias);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> CollectionExpressionBase<?,T> union(Operator<Object> op, List<? extends SubQueryExpression<?>> sq) {
        Expression<?> rv = sq.get(0);
        if (sq.size() == 1 && !CollectionExpression.class.isInstance(rv)) {
            return new ListSubQuery(rv.getType(), sq.get(0).getMetadata());
        } else {
            Class<?> elementType = sq.get(0).getType();
            if (rv instanceof CollectionExpression) {
                elementType = ((CollectionExpression)rv).getParameter(0);
            }
            for (int i = 1; i < sq.size(); i++) {
                rv = CollectionOperation.create(op, (Class)elementType, rv, sq.get(i));
            }
            return (CollectionExpressionBase<?,T>)rv;
        }
    }

    public <T> CollectionExpressionBase<?,T> union(List<? extends SubQueryExpression<T>> sq) {
        return union(SQLOps.UNION, sq);
    }

    @SuppressWarnings("unchecked")
	public <T> CollectionExpressionBase<?,T> union(ListSubQuery<T>... sq) {
        return union(SQLOps.UNION, Arrays.asList(sq));
    }

    @SuppressWarnings("unchecked")
    public <T> CollectionExpressionBase<?,T> union(SubQueryExpression<T>... sq) {
        return union(SQLOps.UNION, Arrays.asList(sq));
    }

    public <T> CollectionExpressionBase<?,T> unionAll(List<? extends SubQueryExpression<T>> sq) {
        return union(SQLOps.UNION_ALL, sq);
    }

    @SuppressWarnings("unchecked")
    public <T> CollectionExpressionBase<?,T> unionAll(ListSubQuery<T>... sq) {
        return union(SQLOps.UNION_ALL, Arrays.asList(sq));
    }

    @SuppressWarnings("unchecked")
    public <T> CollectionExpressionBase<?,T> unionAll(SubQueryExpression<T>... sq) {
        return union(SQLOps.UNION_ALL, Arrays.asList(sq));
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public Q withRecursive(Path<?> alias, SubQueryExpression<?> query) {
        queryMixin.addFlag(new QueryFlag(QueryFlag.Position.WITH, SQLTemplates.RECURSIVE));
        return with(alias, query);
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public Q withRecursive(Path<?> alias, Expression<?> query) {
        queryMixin.addFlag(new QueryFlag(QueryFlag.Position.WITH, SQLTemplates.RECURSIVE));
        return with(alias, query);
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public WithBuilder<Q> withRecursive(Path<?> alias, Path<?>... columns) {
        queryMixin.addFlag(new QueryFlag(QueryFlag.Position.WITH, SQLTemplates.RECURSIVE));
        return with(alias, columns);
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public Q with(Path<?> alias, SubQueryExpression<?> target) {
        Expression<?> expr = OperationImpl.create(alias.getType(), SQLOps.WITH_ALIAS, alias, target);
        return queryMixin.addFlag(new QueryFlag(QueryFlag.Position.WITH, expr));
    }

    @Override
    @WithBridgeMethods(value=AbstractAsyncSQLQuery.class, castRequired=true)
    public Q with(Path<?> alias, Expression<?> query) {
        Expression<?> expr = OperationImpl.create(alias.getType(), SQLOps.WITH_ALIAS, alias, query);
        return queryMixin.addFlag(new QueryFlag(QueryFlag.Position.WITH, expr));
    }

    @Override
    public WithBuilder<Q> with(Path<?> alias, Path<?>... columns) {
        Expression<?> columnsCombined = ExpressionUtils.list(Object.class, columns);
        Expression<?> aliasCombined = Expressions.operation(alias.getType(), SQLOps.WITH_COLUMNS, alias, columnsCombined);
        return new WithBuilder<Q>(queryMixin, aliasCombined);
    }

    public QueryMetadata getMetadata() {
        return queryMixin.getMetadata();
    }
    

    @Override
    public abstract Q clone();

	@Override
	public Q groupBy(Expression<?>... o) {
		return queryMixin.groupBy(o);
	}

	@Override
	public Q having(Predicate... o) {
		return queryMixin.having(o);
	}

	@Override
	public Q limit(long limit) {
		return queryMixin.limit(limit);
	}

	@Override
	public Q offset(long offset) {
		return queryMixin.offset(offset);
	}

	@Override
	public Q restrict(QueryModifiers modifiers) {
		return queryMixin.restrict(modifiers);
	}

	@Override
	public Q orderBy(OrderSpecifier<?>... o) {
		return queryMixin.orderBy(o);
	}

	@Override
	public <T> Q set(ParamExpression<T> param, T value) {
		return queryMixin.set(param, value);
	}

	@Override
	public Q distinct() {
		return queryMixin.distinct();
	}

	@Override
	public Q where(Predicate... o) {
		return queryMixin.where(o);
	}
    
}

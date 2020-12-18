package nl.idgis.publisher.provider.database;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.typesafe.config.Config;
import nl.idgis.publisher.database.JdbcTransaction;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.StreamingQuery;
import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.provider.database.messages.*;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class AbstractDatabaseTransaction extends JdbcTransaction {

	AbstractDatabaseTransaction(Config config, Connection connection) {
		super(config, connection);
	}
	
	public static Props props(Config config, Connection connection) {
		return Props.create(AbstractDatabaseTransaction.class, config, connection);
	}

	@Override
	protected Object executeQuery(Query query) throws Exception {
		if(query instanceof DescribeTable) {
			return handleDescribeTable((DescribeTable)query);
		} else if(query instanceof PerformCount){
			return handlePerformCount((PerformCount)query);
		} else {
			return null;
		}
	}
	
	@Override
	protected ActorRef executeQuery(StreamingQuery query) throws Exception {
		if(query instanceof FetchTable) {
			return handleFetchTable((FetchTable)query);
		} else {
			return null;
		}
	}
	
	abstract Object handleDescribeTable(DescribeTable query) throws SQLException;

	abstract Object handlePerformCount(PerformCount query) throws SQLException;
	
	static void writeFilter(Filter filter, StringBuilder sb) {
		if(filter instanceof CompoundFilter) {
			CompoundFilter compoundFilter = (CompoundFilter)filter;
			String filterSeparator = " " + compoundFilter.getOperator() + " ";
			
			String separator = "";
			for(Filter compoundFilterItem : compoundFilter.getFilters()) {
				sb
					.append(separator)
					.append("(");
				writeFilter(compoundFilterItem, sb);
				sb.append(")");
				
				separator = filterSeparator;
			}
		} else if(filter instanceof ColumnFilter) {
			ColumnFilter columnFilter = (ColumnFilter)filter;
			AbstractDatabaseColumnInfo column = columnFilter.getColumn();
			
			sb
				.append("\"")
				.append(column.getName())
				.append("\" ")
				.append(columnFilter.getOperator())
				.append(" ");
			
			columnFilter.getOperand().ifPresent(operand -> {
				if(column.getType() == Type.TEXT) {
					sb
						.append("'")
						.append(operand.toString().replace("'", "''"))
						.append("'");
				} else {
					sb.append(operand);
				}
			});
		} else {
			throw new IllegalArgumentException("unknown filter type: " + filter.getClass().getCanonicalName());
		}
	}

	abstract ActorRef handleFetchTable(FetchTable msg) throws SQLException;
}

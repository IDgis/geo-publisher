package nl.idgis.publisher.domain.query;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.joda.time.LocalDateTime;

import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Issue;

public class ListIssues implements DomainQuery<Page<Issue>> {
	private static final long serialVersionUID = -6441727944543265410L;
	
	public final static Set<LogLevel> defaultLogLevels = new HashSet<> ();
	
	static {
		defaultLogLevels.add (LogLevel.ERROR);
		defaultLogLevels.add (LogLevel.INFO);
		defaultLogLevels.add (LogLevel.WARNING);
	}
			
	private final Set<LogLevel> logLevels;
	private final Long page;
	private final LocalDateTime since;
	private final Long limit;
	
	public ListIssues () {
		this (null, null);
	}
	
	public ListIssues (final Set<LogLevel> logLevels) {
		this (logLevels, null);
	}
	
	public ListIssues (final Set<LogLevel> logLevels, final Long page) {
		this (logLevels, null, page, null);
	}
	
	public ListIssues (final Set<LogLevel> logLevels, final LocalDateTime since, final Long page, final Long limit) {
		this.logLevels = new HashSet<> (logLevels == null ? defaultLogLevels : logLevels);
		this.page = page;
		this.since = since;
		this.limit = limit;
	}

	public Long getPage () {
		return page;
	}
	
	public Set<LogLevel> getLogLevels () {
		return Collections.unmodifiableSet (logLevels);
	}

	public LocalDateTime getSince () {
		return since;
	}

	public Long getLimit () {
		return limit;
	}
}

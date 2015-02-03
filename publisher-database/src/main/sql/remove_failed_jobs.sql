delete from publisher.import_job_column
where import_job_id in (
	select id from publisher.import_job
	where job_id in (
		select job_id from publisher.job_state js
		where js.state in ('ABORTED', 'FAILED')
	)
);

delete from publisher.import_job
where job_id in (
	select job_id from publisher.job_state js
	where js.state in ('ABORTED', 'FAILED')
);

delete from publisher.job_state js
where job_id in (
	select id from publisher.job j
	where type = 'IMPORT'
	and not exists (
		select from publisher.import_job ij
		where ij.job_id = j.id
	)
);

delete from publisher.job j
where type = 'IMPORT'
and not exists (
	select from publisher.import_job ij
	where ij.job_id = j.id
);
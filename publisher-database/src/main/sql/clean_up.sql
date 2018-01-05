begin;

-- TODO: fix this query
create temp table keep on commit drop as (
select distinct on (source_dataset_id) id
from publisher.source_dataset_version sdv
where exists (
	select from publisher.import_job ij
	where ij.source_dataset_version_id = sdv.id
	and exists (
		select from publisher.job_state js
		where js.job_id = ij.job_id
		and state = 'SUCCEEDED'
	)
)
order by source_dataset_id, create_time desc)
union (
select distinct on (source_dataset_id) id
from publisher.source_dataset_version sdv
where exists (
	select from publisher.import_job ij
	where ij.source_dataset_version_id = sdv.id
	and exists (
		select from publisher.job_state js
		where js.job_id = ij.job_id
		and state in ('FAILED', 'ABORTED')
	)
)
order by source_dataset_id, create_time desc)
union (
select distinct on (source_dataset_id) id
from publisher.source_dataset_version sdv
order by source_dataset_id, create_time desc);

delete from publisher.import_job_column ijc
where exists (
	select from publisher.import_job ij
	where ij.id = ijc.import_job_id
	and not exists (
		select from keep
		where ij.source_dataset_version_id = id
	)
);

delete from publisher.job_log jl
where exists (
	select from publisher.import_job ij
	join publisher.job_state js on js.job_id = ij.job_id
	where jl.job_state_id = js.job_id
	and not exists (
		select from keep
		where ij.source_dataset_version_id = id
	)
);

delete from publisher.job_state js
where exists (
	select from publisher.import_job ij
	where js.job_id = ij.job_id
	and not exists (
		select from keep
		where ij.source_dataset_version_id = id
	)
);

delete from publisher.import_job ij
where not exists (
	select from keep
	where ij.source_dataset_version_id = id
);

delete from publisher.source_dataset_version_log sdvl
where not exists (
	select from keep
	where sdvl.source_dataset_version_id = id
);


delete from publisher.source_dataset_version_column sdvc
where not exists (
	select from keep
	where sdvc.source_dataset_version_id = id
);

delete from publisher.source_dataset_version sdv
where not exists (
	select from keep
	where sdv.id = id
);
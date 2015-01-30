
create or replace view publisher.dataset_column_diff as
select ij.dataset_id, 'REMOVE' diff, ijc.name, ijc.data_type from publisher.import_job_column ijc
join publisher.import_job ij on ij.id = ijc.import_job_id
where not exists ( -- imported column but no longer part of configuration 
	select * from publisher.dataset d
	join publisher.dataset_column dc on dc.dataset_id = d.id
	where d.id = ij.dataset_id and dc.name = ijc.name and dc.data_type = ijc.data_type
) and ij.id = ( -- last import job for dataset
	select max(id) 
	from publisher.import_job ij_max
	where ij_max.dataset_id = ij.dataset_id
	and exists (
		select * from publisher.job_state js
		where js.job_id = ij_max.job_id 
		and js.state in ('SUCCEEDED', 'ABORTED', 'FAILED')
	)
)
union all
select dc.dataset_id, 'ADD' diff, dc.name, dc.data_type from publisher.dataset_column dc
join publisher.dataset d on d.id = dc.dataset_id
where not exists ( -- part of configuration but not imported
	select * from publisher.import_job ij
	join publisher.import_job_column ijc on ijc.import_job_id = ij.id
	where ij.dataset_id = d.id and ijc.name = dc.name and ijc.data_type = dc.data_type
	and ij.id = ( -- last import job for dataset
		select max(id) 
		from publisher.import_job ij_max
		where ij_max.dataset_id = ij.dataset_id
		and exists (
			select * from publisher.job_state js
			where js.job_id = ij_max.job_id 
			and js.state in ('SUCCEEDED', 'ABORTED', 'FAILED')
		)
	)
) and exists ( -- is dataset imported
	select * from publisher.import_job ij
	where ij.dataset_id = d.id
	and exists (
		select * from publisher.job_state js
		where js.job_id = ij.job_id 
		and js.state in ('SUCCEEDED', 'ABORTED', 'FAILED')
	)
);

create or replace view publisher.source_dataset_column_diff as
select ij.dataset_id, 'REMOVE' diff, imported_sdvc.name, imported_sdvc.data_type from publisher.import_job ij
join publisher.source_dataset_version_column imported_sdvc on imported_sdvc.source_dataset_version_id = ij.source_dataset_version_id
join publisher.source_dataset_version sdv on sdv.id = imported_sdvc.source_dataset_version_id
where not exists ( -- in imported version but not in latest version
	select * from publisher.source_dataset_version_column latest_sdvc
	where latest_sdvc.name = imported_sdvc.name and latest_sdvc.data_type = imported_sdvc.data_type
	and latest_sdvc.source_dataset_version_id = ( -- latest version of the same source dataset
		select max(id) 
		from publisher.source_dataset_version max_sdv
		where max_sdv.source_dataset_id = sdv.source_dataset_id)
) and ij.id = ( -- last import job for dataset
	select max(id) from publisher.import_job ij_max
	where ij_max.dataset_id = ij.dataset_id
	and exists (
		select * from publisher.job_state js
		where js.job_id = ij_max.job_id 
		and js.state in ('SUCCEEDED', 'ABORTED', 'FAILED')
	)
)
union all
select ij.dataset_id, 'ADD' diff, latest_sdvc.name, latest_sdvc.data_type from publisher.import_job ij
join publisher.dataset d on d.id = ij.dataset_id
join publisher.source_dataset sd on sd.id = d.source_dataset_id
join publisher.source_dataset_version sdv on sdv.source_dataset_id = sd.id
join publisher.source_dataset_version_column latest_sdvc on latest_sdvc.source_dataset_version_id = sdv.id
where not exists ( -- in latest version but not in imported version
	select * from publisher.source_dataset_version_column imported_sdvc
	where imported_sdvc.name = latest_sdvc.name and imported_sdvc.data_type = latest_sdvc.data_type
	and imported_sdvc.source_dataset_version_id = ij.source_dataset_version_id
) and latest_sdvc.source_dataset_version_id = ( -- latest version of the source dataset
	select max(id) 
	from publisher.source_dataset_version max_sdv
	where max_sdv.source_dataset_id = sdv.source_dataset_id
) and ij.id = ( -- last import job for dataset
	select max(id) from publisher.import_job ij_max
	where ij_max.dataset_id = ij.dataset_id
	and exists (
		select * from publisher.job_state js
		where js.job_id = ij_max.job_id 
		and js.state in ('SUCCEEDED', 'ABORTED', 'FAILED')
	)
);

drop view publisher.dataset_import_job_column_union;
drop view publisher.source_dataset_import_job_column_union;

insert into publisher.version(id) values(29);

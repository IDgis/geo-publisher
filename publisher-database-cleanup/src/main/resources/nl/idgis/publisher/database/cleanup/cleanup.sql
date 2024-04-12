begin;

with job_ids as (
	select distinct ij.job_id from publisher.import_job ij
	where (ij.id < (select last_imported_id
			from (select dataset_id, max(ij2.id) as last_imported_id
				from publisher.import_job ij2
				join publisher.job_state js on js.job_id = ij2.job_id
				join publisher.notification n on n.job_id = ij2.job_id 
				join publisher.notification_result nr on nr.notification_id = n.id 
				where js.state = 'SUCCEEDED' and nr.result = 'OK'
				group by dataset_id) as info
			where info.dataset_id = ij.dataset_id))
), notification_ids as (select n.id as notification_id from publisher.notification n where n.job_id in (select job_id from job_ids))
delete from publisher.notification_result nr where nr.notification_id in (select notification_id from notification_ids);

with job_ids as (
	select distinct ij.job_id from publisher.import_job ij
	where (ij.id < (select last_imported_id
			from (select dataset_id, max(ij2.id) as last_imported_id
				from publisher.import_job ij2
				join publisher.job_state js on js.job_id = ij2.job_id
				join publisher.notification n on n.job_id = ij2.job_id 
				join publisher.notification_result nr on nr.notification_id = n.id 
				where js.state = 'SUCCEEDED' and nr.result = 'OK'
				group by dataset_id) as info
			where info.dataset_id = ij.dataset_id))
)
delete from publisher.notification n where n.job_id in (select job_id from job_ids);

with job_ids as (
	select distinct ij.job_id from publisher.import_job ij
	where (ij.id < (select last_imported_id
			from (select dataset_id, max(ij2.id) as last_imported_id
				from publisher.import_job ij2
				join publisher.job_state js on js.job_id = ij2.job_id
				join publisher.notification n on n.job_id = ij2.job_id 
				join publisher.notification_result nr on nr.notification_id = n.id 
				where js.state = 'SUCCEEDED' and nr.result = 'OK'
				group by dataset_id) as info
			where info.dataset_id = ij.dataset_id))
)
delete from publisher.job j where j.id in (select job_id from job_ids);

with sdv_ids as (
	select sdv.id as sdv_id
	from publisher.source_dataset sd
	join publisher.source_dataset_version sdv on sdv.source_dataset_id = sd.id
	where sdv.id != (
		select max(id)
		from publisher.source_dataset_version sdv2
		where sdv2.source_dataset_id = sd.id
	) and not exists (select *
		from publisher.import_job ij
		join publisher.source_dataset_version sdv2 on sdv2.id = ij.source_dataset_version_id
		where sdv2.id = sdv.id
	)
)
delete from publisher.source_dataset_version_log where source_dataset_version_id in (select sdv_id from sdv_ids);

with sdv_ids as (
	select sdv.id as sdv_id
	from publisher.source_dataset sd
	join publisher.source_dataset_version sdv on sdv.source_dataset_id = sd.id
	where sdv.id != (
		select max(id)
		from publisher.source_dataset_version sdv2
		where sdv2.source_dataset_id = sd.id
	) and not exists (select *
		from publisher.import_job ij
		join publisher.source_dataset_version sdv2 on sdv2.id = ij.source_dataset_version_id
		where sdv2.id = sdv.id
	)
)
delete from publisher.source_dataset_version_column where source_dataset_version_id in (select sdv_id from sdv_ids);

with sdv_ids as (
	select sdv.id as sdv_id
	from publisher.source_dataset sd
	join publisher.source_dataset_version sdv on sdv.source_dataset_id = sd.id
	where sdv.id != (
		select max(id)
		from publisher.source_dataset_version sdv2
		where sdv2.source_dataset_id = sd.id
	) and not exists (select *
		from publisher.import_job ij
		join publisher.source_dataset_version sdv2 on sdv2.id = ij.source_dataset_version_id
		where sdv2.id = sdv.id
	)
)
delete from publisher.source_dataset_version where id in (select sdv_id from sdv_ids);

commit;
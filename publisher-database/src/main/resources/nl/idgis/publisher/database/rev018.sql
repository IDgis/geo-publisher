create or replace view publisher.dataset_status as
select 
	d.id,
	d.identification,
	exists (
		select * from publisher.last_import_job
		where dataset_id = d.id) imported,
	exists (
		select * from publisher.dataset_column_diff
		where dataset_id = d.id) columns_changed,
	exists (
		select * from publisher.last_service_job
		where dataset_id = d.id) service_created,
	exists (
		select * from publisher.source_dataset_column_diff
		where dataset_id = d.id) source_dataset_columns_changed,
	(	
		select sdv0.revision != sdv1.revision
		from publisher.last_import_job lij
		join publisher.import_job ij on ij.job_id = lij.job_id
		join publisher.source_dataset_version sdv0 on sdv0.id = ij.source_dataset_version_id
		join publisher.last_source_dataset_version lsdv on lsdv.dataset_id = ij.dataset_id
		join publisher.source_dataset_version sdv1 on sdv1.id = lsdv.source_dataset_version_id
		where lij.dataset_id = d.id) source_dataset_revision_changed,
	(
		select sdv.source_dataset_id != d.source_dataset_id		
		from publisher.last_import_job lij
		join publisher.import_job ij on ij.job_id = lij.job_id
		join publisher.source_dataset_version sdv on sdv.id = ij.source_dataset_version_id
		where lij.dataset_id = d.id
	) source_dataset_changed,
	(
		select ij.filter_conditions != d.filter_conditions
		from publisher.last_import_job lij
		join publisher.import_job ij on ij.job_id = lij.job_id
		where lij.dataset_id = d.id		
	) filter_condition_changed	
from publisher.dataset d;


create or replace view publisher.last_import_job as
select 
	j0.id job_id, 
	d0.id dataset_id, 

	js.create_time finish_time, 
	js.state finish_state 
	
from publisher.import_job ij0
join publisher.job j0 on j0.id = ij0.job_id
join publisher.job_state js on js.job_id = j0.id
join publisher.dataset d0 on d0.id = ij0.dataset_id
where js.state in ('SUCCEEDED', 'ABORTED', 'FAILED')
and not exists (
	select * 
	from publisher.import_job ij1	
	join publisher.job j1 on j1.id = ij1.job_id
	join publisher.job_state js1 on ij1.id = js1.job_id
	join publisher.dataset d1 on d1.id = ij1.dataset_id
	where d0.id = d1.id 
	and js1.state in ('SUCCEEDED', 'ABORTED', 'FAILED')
	and j0.create_time < j1.create_time
	and exists (
		select * 
		from publisher.job_state js
		where js.job_id = j1.id
		and js.state in ('SUCCEEDED', 'ABORTED', 'FAILED')
		)
);



insert into publisher.version(id) values(18);

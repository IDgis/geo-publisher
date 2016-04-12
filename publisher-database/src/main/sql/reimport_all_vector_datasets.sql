begin;

with dataset_ids as (
	select ds.id dataset_id from publisher.dataset_status ds
	join publisher.last_source_dataset_version lsdv on lsdv.dataset_id = ds.id
	join publisher.source_dataset_version sdv on sdv.id = lsdv.source_dataset_version_id
	where ds.imported and sdv.type = 'VECTOR'
	and not exists (
		select * from publisher.import_job ij
		where ij.dataset_id = ds.id
		and not exists (
			select * from publisher.job_state js
			where js.job_id = ij.job_id
		)
	)
), job_ids as (
	insert into publisher.job("type")
	select 'IMPORT'
	from dataset_ids
	returning id job_id
), job_ids_numbered as (
	select job_id, row_number() over() job_number 
	from job_ids
), dataset_ids_numbered as (
	select dataset_id, row_number() over() dataset_number
	from dataset_ids
), job_dataset_ids as (
	insert into publisher.import_job(job_id, dataset_id, source_dataset_version_id, filter_conditions)
	select 
		job_id, 
		dataset_id, 
		(select source_dataset_version_id from publisher.last_source_dataset_version where d.dataset_id = dataset_id),
		(select filter_conditions from publisher.dataset where id = dataset_id)
	from dataset_ids_numbered d, job_ids_numbered
	where job_number = dataset_number
	returning id import_job_id, dataset_id
)
insert into publisher.import_job_column(import_job_id, index, name, data_type)
select jdi.import_job_id, sdvc.index, sdvc.name, sdvc.data_type from job_dataset_ids jdi
join publisher.last_source_dataset_version lsdv on lsdv.dataset_id = jdi.dataset_id
join publisher.source_dataset_version_column sdvc on sdvc.source_dataset_version_id = lsdv.source_dataset_version_id;

commit;
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
		select * from publisher.last_service_job lsj
		where dataset_id = d.id
		and not exists (
			select * from publisher.last_import_job lij
			where dataset_id = d.id
			and lij.finish_time > lsj.finish_time
		)) service_created,
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

create or replace view publisher.last_service_job as
select 
	j0.id job_id, 
	d0.id dataset_id, 

	js.create_time finish_time, 
	js.state finish_state 
	
from publisher.service_job sj0
join publisher.job j0 on j0.id = sj0.job_id
join publisher.job_state js on js.job_id = j0.id
join publisher.dataset d0 on d0.id = sj0.dataset_id
where js.state in ('SUCCEEDED', 'ABORTED', 'FAILED')
and not exists (
 	select * 
 	from publisher.service_job sj1	
 	join publisher.job j1 on j1.id = sj1.job_id
 	join publisher.dataset d1 on d1.id = sj0.dataset_id
 	where d0.id = d1.id
 	and j0.create_time < j1.create_time
 	and exists (
 		select * 
 		from publisher.job_state js
 		where js.job_id = j1.id)
);

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
	join publisher.job_state js1 on j1.id = js1.job_id
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

create or replace view publisher.dataset_column_diff as
select d.id dataset_id, 'ADD' diff, dc.name, dc.data_type from publisher.dataset d
join publisher.dataset_column dc on dc.dataset_id = d.id
where not exists (
	select * from publisher.dataset_import_job_column_union u
	where u.name = dc.name and u.data_type = dc.data_type
)
and exists (
	select * from publisher.last_import_job lij
	where lij.dataset_id = d.id
)
union all
select d.id dataset_id, 'REMOVE' diff, inc.name, inc.data_type from publisher.dataset d
join publisher.last_import_job lij on lij.dataset_id = d.id
join publisher.import_job ij on ij.job_id = lij.job_id
join publisher.import_job_column inc on inc.import_job_id = ij.id
where not exists (
	select * from publisher.dataset_import_job_column_union u
	where u.name = inc.name and u.data_type = inc.data_type
)
and exists (
	select * from publisher.last_import_job lij
	where lij.dataset_id = d.id
);

create or replace view publisher.source_dataset_column_diff as
select d.id dataset_id, 'ADD' diff, sdvc.name, sdvc.data_type from publisher.dataset d
join publisher.last_source_dataset_version lsdv on lsdv.dataset_id = d.id
join publisher.source_dataset_version_column sdvc on sdvc.source_dataset_version_id = lsdv.source_dataset_version_id
where not exists (
	select * from publisher.source_dataset_import_job_column_union u
	where u.name = sdvc.name and u.data_type = sdvc.data_type
)
and exists (
	select * from publisher.last_import_job lij
	where lij.dataset_id = d.id
)
union all
select d.id dataset_id, 'REMOVE' diff, sdvc.name, sdvc.data_type from publisher.dataset d
join publisher.last_import_job lij on lij.dataset_id = d.id
join publisher.import_job ij on ij.job_id = lij.job_id
join publisher.source_dataset_version_column sdvc on sdvc.source_dataset_version_id = ij.source_dataset_version_id
where not exists (
	select * from publisher.source_dataset_import_job_column_union u
	where u.name = sdvc.name and u.data_type = sdvc.data_type
)
and exists (
	select * from publisher.last_import_job lij
	where lij.dataset_id = d.id
);

insert into publisher.version(id) values(20);

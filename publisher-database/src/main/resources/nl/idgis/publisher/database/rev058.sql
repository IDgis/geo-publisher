
alter table publisher.service add column wms_metadata_file_identification varchar(36);
alter table publisher.service add column wfs_metadata_file_identification varchar(36);

-- following drop view statements are required in order to be able to 
-- alter columns uuid and file_uuid from publisher.dataset,
-- the usage of 'select * from publisher.dataset' in these views resulted 
-- in dependencies on columns uuid and file_uuid
drop view publisher.dataset_status;
drop view publisher.dataset_column_diff;

alter table publisher.dataset add column metadata_file_identification varchar(36);
alter table publisher.dataset add column metadata_identification varchar(36);

update publisher.dataset
set metadata_file_identification = file_uuid, 
metadata_identification = uuid;

alter table publisher.dataset drop column file_uuid;
alter table publisher.dataset drop column uuid;

-- recreate views with the same definition
create view publisher.dataset_column_diff as
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

create view publisher.dataset_status as 
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
	) filter_condition_changed,
	(
		select sdv.type != 'UNAVAILABLE'
		from publisher.last_source_dataset_version lsdv
		join publisher.source_dataset_version sdv on sdv.id = lsdv.source_dataset_version_id
		where lsdv.dataset_id = d.id
	) source_dataset_available
from publisher.dataset d;

insert into publisher.version(id) values(58);
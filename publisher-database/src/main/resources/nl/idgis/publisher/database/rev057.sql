
drop view publisher.dataset_status;

-- source_dataset_available column added
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

insert into publisher.version(id) values(57);
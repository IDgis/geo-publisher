
create or replace view publisher.dataset_column_diff as
select d.id dataset_id, 'ADD' diff, dc.name, dc.data_type from publisher.dataset d
join publisher.dataset_column dc on dc.dataset_id = d.id
where not exists (
	select * from publisher.dataset_import_job_column_union u
	where u.name = dc.name and u.data_type = dc.data_type and u.dataset_id = d.id
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
	where u.name = inc.name and u.data_type = inc.data_type and u.dataset_id = d.id
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
	where u.name = sdvc.name and u.data_type = sdvc.data_type and u.dataset_id = d.id
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
	where u.name = sdvc.name and u.data_type = sdvc.data_type and u.dataset_id = d.id
)
and exists (
	select * from publisher.last_import_job lij
	where lij.dataset_id = d.id
);

insert into publisher.version(id) values(29);

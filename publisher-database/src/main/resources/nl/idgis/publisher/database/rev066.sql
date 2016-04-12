
alter table publisher.import_job alter column source_dataset_version_id set not null;

insert into publisher.version(id) values(66);


alter table publisher.source_dataset_version add column physical_name text;

insert into publisher.version(id) values(78);

alter table publisher.source_dataset_version add column table_name text;

insert into publisher.version(id) values(78);
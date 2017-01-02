alter table publisher.source_dataset_version_column add column alias text;

insert into publisher.version(id) values(72);
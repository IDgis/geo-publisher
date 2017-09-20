
alter table publisher.source_dataset_version add column refresh_frequency text;

insert into publisher.version(id) values(79);
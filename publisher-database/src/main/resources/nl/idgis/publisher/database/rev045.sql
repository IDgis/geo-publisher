
alter table publisher.source_dataset_version add column alternate_title varchar(200);

insert into publisher.version(id) values(45);

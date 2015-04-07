
alter table publisher.source_dataset_version add column confidential boolean;
update publisher.source_dataset_version set confidential = false;
alter table publisher.source_dataset_version alter column confidential set not null;

insert into publisher.version(id) values(50);
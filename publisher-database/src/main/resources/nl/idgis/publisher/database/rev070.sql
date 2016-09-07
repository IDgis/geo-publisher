
alter table publisher.source_dataset_version add column metadata_confidential boolean;
update publisher.source_dataset_version set metadata_confidential = true;
alter table publisher.source_dataset_version alter column metadata_confidential set not null;

insert into publisher.version(id) values(70);

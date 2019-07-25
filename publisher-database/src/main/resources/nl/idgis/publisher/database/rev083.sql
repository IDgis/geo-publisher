
alter table publisher.source_dataset_version add archived boolean NOT NULL DEFAULT false;

insert into publisher.version(id) values(83);
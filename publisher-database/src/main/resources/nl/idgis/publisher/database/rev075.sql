
alter table publisher.source_dataset_version add wms_only boolean NOT NULL DEFAULT false;
alter table publisher.environment add wms_only boolean NOT NULL DEFAULT false;

insert into publisher.version(id) values(75);
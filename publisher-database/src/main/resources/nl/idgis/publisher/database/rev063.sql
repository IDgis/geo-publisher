
alter table publisher.dataset alter column metadata_file_identification set not null;
alter table publisher.dataset alter column metadata_identification set not null;

alter table publisher.service alter column wms_metadata_file_identification set not null;
alter table publisher.service alter column wfs_metadata_file_identification set not null;

insert into publisher.version(id) values(63);

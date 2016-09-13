
alter table publisher.source_dataset add column metadata_identification varchar(36);
alter table publisher.source_dataset add column metadata_file_identification varchar(36);

-- <PostgreSQL>
update publisher.source_dataset set 
	metadata_identification = uuid_in(md5('metadata' || identification)::cstring),
	metadata_file_identification = uuid_in(md5('metadata_file' || identification)::cstring);
-- </PostgreSQL>

alter table publisher.source_dataset alter column metadata_identification set not null;
alter table publisher.source_dataset alter column metadata_file_identification set not null;

insert into publisher.version(id) values(71);

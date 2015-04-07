
alter table publisher.source_dataset add column external_identification varchar(80);
update publisher.source_dataset set external_identification = identification;
alter table publisher.source_dataset alter column external_identification set not null;

insert into publisher.version(id) values(51);

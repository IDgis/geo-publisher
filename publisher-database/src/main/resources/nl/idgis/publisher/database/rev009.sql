
alter table publisher.source_dataset add column revision timestamp;
update publisher.source_dataset set revision = '-infinity';

alter table publisher.source_dataset alter column revision set not null;

insert into publisher.version(id) values(9);
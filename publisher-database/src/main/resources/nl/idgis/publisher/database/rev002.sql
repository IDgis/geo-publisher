
alter table publisher.version alter column create_time set not null;
alter table publisher.version alter column create_time set default now();

alter table publisher.data_source add column create_time timestamp not null default now();
alter table publisher.data_source add column update_time timestamp;
alter table publisher.data_source add column delete_time timestamp;
alter table publisher.data_source alter column identification set not null;
alter table publisher.data_source alter column name set not null;

alter table publisher.source_dataset add column create_time timestamp not null default now();
alter table publisher.source_dataset add column update_time timestamp;
alter table publisher.source_dataset add column delete_time timestamp;
alter table publisher.source_dataset alter column data_source_id set not null;
alter table publisher.source_dataset alter column identification set not null;
alter table publisher.source_dataset alter column name set not null;

insert into publisher.version(id) values(2);
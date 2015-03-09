alter table publisher.service_job alter column service_id drop not null;
alter table publisher.service_job add column type varchar(80) default 'ENSURE';
alter table publisher.service_job alter column type set not null;

insert into publisher.version(id) values(37);

alter table publisher.service_job alter column service_id drop not null;
alter table publisher.service_job add column type varchar(80) not null;

insert into publisher.version(id) values(37);

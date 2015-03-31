
alter table publisher.service_job add column published boolean;
update publisher.service_job set published = false;
alter table publisher.service_job alter column published set not null;

insert into publisher.version(id) values(49);
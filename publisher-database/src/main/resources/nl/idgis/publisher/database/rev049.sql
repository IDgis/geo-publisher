
alter table publisher.service_job add column published boolean not null;

insert into publisher.version(id) values(49);
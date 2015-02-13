--
-- service column publish
--
alter table publisher.service add column published boolean default true;

insert into publisher.version(id) values(34);
-- ----------------------------------
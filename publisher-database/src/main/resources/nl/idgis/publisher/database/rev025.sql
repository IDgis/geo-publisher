
alter table publisher.dataset add column file_uuid varchar(36);

update publisher.dataset
set file_uuid = 'fix-me';

-- generate fake file_uuid based on identification (PostgreSQL only doesn't work on h2):
-- update publisher.dataset
-- set file_uuid = uuid_in(md5(identification || 'file')::cstring)

alter table publisher.dataset alter column file_uuid set not null;

insert into publisher.version(id) values(25);

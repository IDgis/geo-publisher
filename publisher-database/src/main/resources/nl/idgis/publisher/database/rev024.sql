
alter table publisher.dataset add column uuid varchar(36);

update publisher.dataset
set uuid = 'fix-me';

-- generate fake uuid based on identification (PostgreSQL only doesn't work on h2):
-- update publisher.dataset
-- set uuid = uuid_in(md5(identification)::cstring)

alter table publisher.dataset alter column uuid set not null;

insert into publisher.version(id) values(24);

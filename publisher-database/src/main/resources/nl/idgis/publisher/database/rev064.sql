
-- <PostgreSQL>
alter table publisher.published_service alter column content type text;
-- </PostgreSQL>

insert into publisher.version(id) values(64);

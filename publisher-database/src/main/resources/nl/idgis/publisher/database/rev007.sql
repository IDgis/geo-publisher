
alter table publisher.dataset add column name varchar(200);

update publisher.dataset set name = identification;

alter table publisher.dataset alter column name set not null;

insert into publisher.version(id) values(7);
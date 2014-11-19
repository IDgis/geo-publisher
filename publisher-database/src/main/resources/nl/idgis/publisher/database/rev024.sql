
alter table publisher.dataset add column uuid varchar(36);

update publisher.dataset
set uuid = 'fix-me';

alter table publisher.dataset alter column uuid set not null;

insert into publisher.version(id) values(24);

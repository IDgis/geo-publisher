
alter table publisher.environment add column confidential boolean not null default false; 

insert into publisher.version(id) values(53);


alter table publisher.import_job add column filter_conditions text not null;

insert into publisher.version(id) values(16);

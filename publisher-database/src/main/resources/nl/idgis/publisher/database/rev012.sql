
alter table publisher.dataset add column filter_conditions text not null default '{ "expression": null }';

insert into publisher.version(id) values(12);
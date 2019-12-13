
alter table publisher.generic_layer add usergroups text NOT NULL DEFAULT '[]';

insert into publisher.version(id) values(84);
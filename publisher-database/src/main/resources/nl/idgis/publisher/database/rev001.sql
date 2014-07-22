
drop table publisher.provider;

create table publisher.data_source (
	id serial primary key,
	identification varchar(80) unique,
	name varchar(80)
);

create table publisher.source_dataset (
	id serial primary key,
	data_source_id integer references publisher.data_source(id),
	identification varchar(80) unique,
	name varchar(80)
);

insert into publisher.version(id, create_time) values(1, now());
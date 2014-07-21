create schema publisher;

create table publisher.version(
	id integer primary key,
	create_time timestamp
);

insert into publisher.version(id, create_time) values(0, now());

create table publisher.provider(
	id serial primary key,
	name text
);
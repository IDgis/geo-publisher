
create table publisher.harvest_log (
	id serial primary key,
	create_time timestamp not null default now(),
	dataSource_id integer not null references publisher.data_source(id),
	event varchar(80)
);

create table publisher.import_log (
	id serial primary key,
	create_time timestamp not null default now(),
	dataset_id integer not null references publisher.dataset(id),
	event varchar(80)
);

insert into publisher.version(id) values(6);
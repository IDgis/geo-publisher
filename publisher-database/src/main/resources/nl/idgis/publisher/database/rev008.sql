
drop table publisher.harvest_log;
drop table publisher.import_log;

create table publisher.job (
	id serial primary key,
	type varchar(80) not null,
	create_time timestamp not null default now()
);

create table publisher.job_state (
	id serial primary key,
	job_id integer not null references publisher.job(id),
	state varchar(80) not null,
	create_time timestamp not null default now(),

	constraint unique_state unique(job_id, state)
);

create table publisher.job_log (
	job_state_id integer not null references publisher.job_state(id),
	level varchar(80) not null,
	type varchar(80) not null,
	content varchar(1000) not null,
	create_time timestamp not null default now()
);

create table publisher.import_job (
	job_id integer not null references publisher.job(id),
	dataset_id integer not null references publisher.dataset(id)
);

create table publisher.harvest_job (
	job_id integer not null references publisher.job(id),
	data_source_id integer not null references publisher.data_source(id)
);

insert into publisher.version(id) values(8);
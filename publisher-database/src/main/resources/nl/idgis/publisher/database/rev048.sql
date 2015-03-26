
create table publisher.environment(
	id serial,
	identification varchar(80),
	name varchar(200),

	constraint environment_pk primary key(id)
);

create table publisher.published_service(
	service_id integer,
	create_time timestamp default now(),
	content varchar(131072), -- 128 KiB	

	constraint published_service_pk primary key(service_id),
	constraint published_service_service_fk foreign key(service_id) references publisher.service(id)
);

create table publisher.published_service_environment(
	service_id integer,
	environment_id integer,
	
	constraint published_service_environment_pk primary key(service_id, environment_id),
	constraint published_service_environment_service_id_fk foreign key(service_id) references publisher.published_service(service_id),
	constraint published_service_environment_environment_id foreign key(environment_id) references publisher.environment(id)
);

create table publisher.published_service_style(
	id serial,
	service_id integer,
	identification varchar(80),
	name varchar(80),
	definition text,
	
	constraint published_service_style_pk primary key(id),
	constraint published_service_service_id_fk foreign key(service_id) references publisher.published_service(service_id)
);

insert into publisher.version(id) values(48);
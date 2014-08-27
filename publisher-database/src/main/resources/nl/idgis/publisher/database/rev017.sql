
create table publisher.notification (
	id serial primary key,
	job_id integer references job(id),
	type text not null
);

create table publisher.notification_result (
	notification_id integer not null references notification(id),
	result text not null
);

insert into publisher.version(id) values(17);


create table publisher.harvest_notification(
	id serial primary key,
	notification_type varchar(80) not null,
	create_time timestamp not null default now(),
	source_dataset_id integer not null references publisher.source_dataset(id),
	source_dataset_version_id integer not null references publisher.source_dataset_version(id) on delete cascade,
	done boolean not null
);

insert into publisher.version(id) values(76);
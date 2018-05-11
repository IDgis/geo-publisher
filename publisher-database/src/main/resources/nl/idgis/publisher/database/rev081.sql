
create table publisher.source_dataset_uuid_count (
	source_dataset_id integer not null references publisher.source_dataset(id) on delete cascade,
	count integer not null
);

insert into publisher.version(id) values(81);
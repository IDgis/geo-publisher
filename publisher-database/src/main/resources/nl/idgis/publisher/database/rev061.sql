
create table publisher.source_dataset_metadata(
	source_dataset_id integer references publisher.source_dataset(id) on delete cascade,
	document bytea not null,
	
	unique(source_dataset_id)
);

insert into publisher.version(id) values(61);

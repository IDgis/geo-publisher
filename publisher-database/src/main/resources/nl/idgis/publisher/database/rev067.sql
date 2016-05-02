
create table publisher.source_dataset_metadata_attachment(
	id serial primary key,
	source_dataset_id integer references publisher.source_dataset(id) on delete cascade,	
	identification text,
	content_type text,
	content_disposition text,
	content bytea not null
);

insert into publisher.version(id) values(67);

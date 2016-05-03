
create table publisher.source_dataset_metadata_attachment(
	id serial primary key,
	source_dataset_id integer not null references publisher.source_dataset(id) on delete cascade,
	identification text not null,
	content_type text,
	content_disposition text,
	content bytea not null
);

insert into publisher.version(id) values(67);


create table publisher.source_dataset_column (
	source_dataset_id integer references publisher.source_dataset(id),
	index integer not null,
	identification varchar(80) not null,
	data_type varchar(80) not null,
	
	constraint source_dataset_column_identification unique(source_dataset_id, identification),
	constraint source_dataset_column_index unique(source_dataset_id, index)
);

create table publisher.dataset (
	id serial primary key,
	identification varchar(80) not null unique,
	source_dataset_id integer references publisher.source_dataset(id)
);

create table publisher.dataset_column (
	dataset_id integer references publisher.dataset(id),
	index integer not null,
	identification varchar(80) not null,
	data_type varchar(80) not null,
	
	constraint dataset_column_identification unique(dataset_id, identification),
	constraint dataset_column_index unique(dataset_id, index)
);

insert into publisher.version(id) values(3);

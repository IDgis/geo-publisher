
create table publisher.source_dataset_column (
	source_dataset_id integer references publisher.source_dataset(id),
	index integer not null,
	name varchar(80) not null,
	data_type varchar(80) not null,
	
	constraint source_dataset_column_name unique(source_dataset_id, name),
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
	name varchar(80) not null,
	data_type varchar(80) not null,
	
	constraint dataset_column_name unique(dataset_id, name),
	constraint dataset_column_index unique(dataset_id, index)
);

insert into publisher.version(id) values(3);

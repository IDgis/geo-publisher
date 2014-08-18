
create table publisher.source_dataset_history (
	id serial primary key,
	source_dataset_id integer not null references publisher.source_dataset(id),
	name character varying(200) not null,
	category_id integer not null references publisher.category(id),
	revision timestamp not null,
	time timestamp not null
);

create table publisher.source_dataset_column_history (
	source_dataset_history_id integer not null references publisher.source_dataset_history(id),
	index integer not null,
	name character varying(80) not null,
	data_type character varying(80) not null
);

insert into publisher.version(id) values(10);

create table publisher.dataset_view (
	dataset_id integer references publisher.dataset(id),
	index integer not null,
	name varchar(80) not null,
	data_type varchar(80) not null,
	
	constraint dataset_view_name unique(dataset_id, name),
	constraint dataset_view_index unique(dataset_id, index)
);

create table publisher.dataset_copy (
	dataset_id integer references publisher.dataset(id),
	index integer not null,
	name varchar(80) not null,
	data_type varchar(80) not null,
	
	constraint dataset_copy_name unique(dataset_id, name),
	constraint dataset_copy_index unique(dataset_id, index)
);

insert into publisher.version(id) values(60);

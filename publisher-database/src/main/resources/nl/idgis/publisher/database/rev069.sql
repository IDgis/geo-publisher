
-- All this database rev does is add a delete cascade to the dataset foreign key constraints.
-- This script is somewhat convoluted because existing foreign keys where not explicitly 
-- named and implicit names are different between H2 and PostgreSQL.

alter table publisher.dataset_copy rename to dataset_copy_orig;
alter table publisher.dataset_copy_orig drop constraint dataset_copy_name;
alter table publisher.dataset_copy_orig drop constraint dataset_copy_index;

create table publisher.dataset_copy (
	dataset_id integer,
	index integer not null,
	name varchar(80) not null,
	data_type varchar(80) not null,
	
	constraint dataset_copy_dataset_id_fk foreign key(dataset_id) references publisher.dataset(id) on delete cascade,
	constraint dataset_copy_name unique(dataset_id, name),
	constraint dataset_copy_index unique(dataset_id, index)
);

insert into publisher.dataset_copy(dataset_id, index, name, data_type)
select dataset_id, index, name, data_type from publisher.dataset_copy_orig;

drop table publisher.dataset_copy_orig;

alter table publisher.dataset_view rename to dataset_view_orig;
alter table publisher.dataset_view_orig drop constraint dataset_view_name;
alter table publisher.dataset_view_orig drop constraint dataset_view_index;

create table publisher.dataset_view (
	dataset_id integer,
	index integer not null,
	name varchar(80) not null,
	data_type varchar(80) not null,
	
	constraint dataset_view_dataset_id_fk foreign key(dataset_id) references publisher.dataset(id) on delete cascade,
	constraint dataset_view_name unique(dataset_id, name),
	constraint dataset_view_index unique(dataset_id, index)
);

insert into publisher.dataset_view(dataset_id, index, name, data_type)
select dataset_id, index, name, data_type from publisher.dataset_view_orig;

drop table publisher.dataset_view_orig;

insert into publisher.version(id) values(69);

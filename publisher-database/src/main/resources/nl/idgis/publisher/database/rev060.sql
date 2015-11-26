
create table publisher.dataset_view (
	dataset_id integer references publisher.dataset(id),
	index integer not null,
	name varchar(80) not null,
	data_type varchar(80) not null,
	
	constraint dataset_view_name unique(dataset_id, name),
	constraint dataset_view_index unique(dataset_id, index)
);

-- <PostgreSQL>
insert into publisher.dataset_view(dataset_id, index, name, data_type)
select dataset_id, index, name, data_type
from publisher.dataset_column dc
where exists (
	select * from information_schema.tables t
	join publisher.dataset d on d.identification = t.table_name
	where t.table_schema = 'data' and dc.dataset_id = d.id
);
-- </PostgreSQL>

create table publisher.dataset_copy (
	dataset_id integer references publisher.dataset(id),
	index integer not null,
	name varchar(80) not null,
	data_type varchar(80) not null,
	
	constraint dataset_copy_name unique(dataset_id, name),
	constraint dataset_copy_index unique(dataset_id, index)
);

insert into publisher.version(id) values(60);

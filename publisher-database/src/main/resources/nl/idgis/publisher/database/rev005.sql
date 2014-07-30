
create table publisher.category (
	id serial primary key,
	identification varchar(120) not null unique,
	name varchar(120) not null
);

alter table publisher.source_dataset 
	add column category_id integer;

alter table publisher.source_dataset 
	add constraint source_dataset_category_id_fkey 
		foreign key(category_id) references publisher.category(id);
		
alter table publisher.source_dataset alter column category_id set not null;

insert into publisher.version(id) values(5);
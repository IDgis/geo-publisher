
alter table publisher.import_job add column id serial;
alter table publisher.import_job 
	add constraint import_job_pkey primary key(id); 

create table publisher.import_job_column (
	import_job_id integer not null references publisher.import_job(id),
	index integer not null,
  	name varchar(80) not null,
	data_type varchar(80) not null
);

insert into publisher.version(id) values(14);

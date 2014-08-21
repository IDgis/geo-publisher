
create table publisher.service_job (
	job_id integer not null references publisher.job(id),
	dataset_id integer not null references publisher.dataset(id)
);

insert into publisher.version(id) values(11);
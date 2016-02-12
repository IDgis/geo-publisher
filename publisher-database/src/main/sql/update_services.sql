begin;

create temp table service_update(
	service_id integer,
	published boolean
) on commit drop;

insert into service_update(service_id, published)
select id, true
from publisher.service;

insert into service_update(service_id, published)
select id, false
from publisher.service;

with job_ids as (
	insert into publisher.job("type")
	select 'SERVICE'
	from service_update
	returning id job_id),
job_ids_numbered as (
	select job_id, row_number() over() job_number 
	from job_ids),
service_update_numbered as (
	select *, row_number() over() service_update_number
	from service_update
)
insert into publisher.service_job
select job_id, service_id, 'ENSURE'::varchar(80), published
from job_ids_numbered, service_update_numbered
where job_number = service_update_number;

commit;
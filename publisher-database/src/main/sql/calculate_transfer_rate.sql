with import_job_info as (
select d.identification, (
	select create_time 
	from publisher.job_state js 
	where js.state = 'STARTED' 
	and js.job_id = ij.job_id) start_time, (
	select create_time 
	from publisher.job_state js 
	where js.state = 'SUCCEEDED' 
	and js.job_id = ij.job_id) finish_time
from publisher.import_job ij
join publisher.dataset d on d.id = ij.dataset_id),
dataset_info as (
select 
	iji.identification, 
	iji.finish_time - iji.start_time import_time, 
	pg_relation_size(c.oid) size
from import_job_info iji
join pg_class c on c.relname =  iji.identification and c.relkind = 'r'
where iji.start_time is not null
and iji.finish_time is not null)
select
	di.identification,
	di.import_time,
	pg_size_pretty(di.size) size,
	pg_size_pretty(round(size / extract('epoch' from di.import_time))::bigint) || '/s' transfer_rate
from dataset_info di;
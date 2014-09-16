
create or replace view publisher.last_import_job as
select 
	job_id, 
	dataset_id, 

	js.create_time finish_time, 
	js.state finish_state 

from publisher.job_state js
join (
	select ij.dataset_id, max(js.id) job_state_id
	from publisher.job_state js
	join publisher.import_job ij on ij.job_id = js.job_id
	where js.state in ('SUCCEEDED', 'ABORTED', 'FAILED')
	group by ij.dataset_id) max_js on max_js.job_state_id = js.id;

create or replace view publisher.last_service_job as
select 
	job_id, 
	dataset_id, 

	js.create_time finish_time, 
	js.state finish_state 

from publisher.job_state js
join (
	select sj.dataset_id, max(js.id) job_state_id
	from publisher.job_state js
	join publisher.service_job sj on sj.job_id = js.job_id
	where js.state in ('SUCCEEDED', 'ABORTED', 'FAILED')
	group by sj.dataset_id) max_js on max_js.job_state_id = js.id;

insert into publisher.version(id) values(22);

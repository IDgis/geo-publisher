
alter table publisher.job_log alter column content drop not null;

create or replace view publisher.last_service_job as
select 
	job_id, 
	dataset_id, 

	js0.create_time finish_time, 
	js0.state finish_state ,

	exists (
		select *
		from publisher.job_log jl
		join publisher.job_state js1 on js1.id = jl.job_state_id
		where js1.job_id = js0.job_id and jl.type = 'VERIFIED'
	) verified,

	exists (
		select *
		from publisher.job_log jl
		join publisher.job_state js1 on js1.id = jl.job_state_id
		where js1.job_id = js0.job_id and jl.type = 'ADDED'
	) added

from publisher.job_state js0
join (
	select sj.dataset_id, max(js.id) job_state_id
	from publisher.job_state js
	join publisher.service_job sj on sj.job_id = js.job_id
	where js.state in ('SUCCEEDED', 'ABORTED', 'FAILED')
	group by sj.dataset_id) max_js on max_js.job_state_id = js0.id;

insert into publisher.version(id) values(23);

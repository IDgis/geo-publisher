
alter table publisher.service_job drop constraint if exists service_job_job_id_fkey; -- PostgreSQL
alter table publisher.service_job drop constraint if exists service_job_job_id_fk; -- H2

alter table publisher.service_job 
	add constraint service_job_job_id_fkey foreign key(job_id)
    references publisher.job(id) on delete cascade;
    
alter table publisher.job_state drop constraint if exists job_state_job_id_fkey; -- PostgreSQL
alter table publisher.job_state drop constraint if exists job_state_job_id_fk; -- H2
    
alter table publisher.job_state
	add constraint job_state_job_id_fkey foreign key(job_id)
	references publisher.job(id) on delete cascade;
	
alter table publisher.job_log drop constraint if exists job_log_job_state_id_fkey; -- PostgreSQL
alter table publisher.job_log drop constraint if exists job_log_job_state_id_fkey; -- H2
	
alter table publisher.job_log
	add constraint job_log_job_state_id_fkey foreign key(job_state_id)
	references publisher.job_state(id) on delete cascade;

insert into publisher.version(id) values(44);


delete from publisher.job
where id in (select job_id from publisher.service_job);

drop table publisher.service_job;

insert into publisher.version(id) values(85);


insert into publisher.job(type) values('HARVEST');
insert into publisher.harvest_job(job_id, data_source_id)
select j.id, ds.id from publisher.job j
join publisher.data_source ds on true;
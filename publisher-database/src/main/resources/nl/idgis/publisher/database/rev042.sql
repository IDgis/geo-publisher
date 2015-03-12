
alter table publisher.service_job add constraint service_job_service_id_fk foreign key (service_id) references publisher.service(id);

insert into publisher.version(id) values(42);

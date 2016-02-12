
alter table publisher.published_service add column environment_id integer;
alter table publisher.published_service add constraint published_service_environment_fk 
	foreign key(environment_id) references publisher.environment(id);

update publisher.published_service ps
set environment_id = (
	select environment_id 
	from publisher.published_service_environment pse 
	where pse.service_id = ps.service_id);
	
alter table publisher.published_service alter column environment_id set not null;

drop table publisher.published_service_environment;

insert into publisher.version(id) values(62);

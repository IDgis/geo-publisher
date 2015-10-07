
alter table publisher.published_service add column title text;
alter table publisher.published_service add column alternate_title text;
alter table publisher.published_service add column abstract text;

update publisher.published_service
set title = (
	select gl.title
	from publisher.service s
	join publisher.generic_layer gl on gl.id = s.generic_layer_id
	where s.id = service_id
), alternate_title = (
	select s.alternate_title 
	from publisher.service s	
	where s.id = service_id
), abstract = (
	select gl.abstract 
	from publisher.service s
	join publisher.generic_layer gl on gl.id = s.generic_layer_id
	where s.id = service_id
);

insert into publisher.version(id) values(59);


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

create table publisher.published_service_keyword (
  keyword text not null,
  service_id integer not null references 
  	publisher.published_service(service_id) 
  	on delete cascade
);

insert into publisher.published_service_keyword(keyword, service_id)
select keyword, service_id
from publisher.service_keyword sw
where exists (
	select *
	from publisher.published_service ps
	where ps.service_id = sw.service_id
);

delete from publisher.publisher.published_service_dataset;
alter table publisher.publisher.published_service_dataset add column layer_name text not null;

-- <PostgreSQL>
with recursive layers as (
    select
        service_id,
        json_array_elements(content::json -> 'layers') -> 'layer' layer, 
        0 depth
    from publisher.published_service
    union all
    select
        service_id, 
        json_array_elements(layer -> 'layers') -> 'layer' layer,
        depth + 1 depth
    from layers
)
insert into publisher.published_service_dataset(service_id, dataset_id, layer_name)
select service_id, dataset_id, layer ->> 'name' layer_name
from layers l
join publisher.generic_layer gl on gl.identification = layer ->> 'id'
join publisher.leaf_layer ll on ll.generic_layer_id = gl.id
group by 1, 2, 3;
-- </PostgreSQL>

insert into publisher.version(id) values(59);

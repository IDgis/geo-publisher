-- 
-- several column renames, column remove
-- 

-- column rename

alter table publisher.layer_style drop column defaultstyle;
alter table publisher.layer_style add column default_style boolean default false;

alter table publisher.layer_structure drop column layerorder;
alter table publisher.layer_structure add column layer_order integer default 0;

-- table service 
drop table publisher.service;
create table publisher.service(
	id serial primary key,
	identification text,
	name text,
	title text,
	alternate_title text,
	abstract text,
	keywords text,
	metadata text,
	watermark text,
	rootgroup_id integer references publisher.generic_layer(id),
	default_category_id integer references publisher.category(id),
	constants_id integer references publisher.constants(id),
	-- add watermark
	watermark_enabled boolean default false,
	watermark_url text,
	watermark_transparency integer,
	watermark_position text
);



-- column remove
alter table publisher.leaf_layer drop column identification;


insert into publisher.version(id) values(33);
-- ----------------------------------
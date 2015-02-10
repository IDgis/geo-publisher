-- 
-- several fixes 
-- 

-- add column published
alter table publisher.service add column published boolean default false;

-- add column published
alter table publisher.generic_layer add column published boolean default false;


-- remove columns that are shared with generic_layer
alter table publisher.leaf_layer drop column name;
alter table publisher.leaf_layer drop column title;
alter table publisher.leaf_layer drop column abstract;

-- make columns identification unique
-- because of H2, column has to be dropped first
-- and the type can not be text
alter table publisher.service drop column identification;
alter table publisher.service add column identification varchar(80) not null;
alter table publisher.service add constraint service_identification_unique unique (identification);

alter table publisher.service drop column alternatetitle;
alter table publisher.service add column alternate_title text;

alter table publisher.generic_layer drop column identification;
alter table publisher.generic_layer add column identification varchar(80) not null;
alter table publisher.generic_layer add constraint generic_layer_identification_unique unique (identification);

alter table publisher.leaf_layer drop column identification;
alter table publisher.leaf_layer add column identification varchar(80) not null;
alter table publisher.leaf_layer add constraint leaf_layer_identification_unique unique (identification);

alter table publisher.style drop column identification;
alter table publisher.style add column identification varchar(80) not null;
alter table publisher.style add constraint style_identification_unique unique (identification);

-- drop table tiledlayer in order to rename some names 
drop table publisher.tiledlayer;
create table publisher.tiled_layer(
	id serial primary key,
	identification varchar(80) not null,
	name text,
	enabled boolean default false,
	mime_formats text,
	meta_width integer,
	meta_height integer,
	expire_cache integer,
	expire_clients integer,
	gutter integer,
	generic_layer_id integer references publisher.generic_layer(id)
);

alter table publisher.tiled_layer add constraint tiled_layer_identification_unique unique (identification);

insert into publisher.version(id) values(32);
-- --------------------
--
-- Several database changes  
--
-- changing columns (removing / renaming / adding)
--
-- tables with new references need drop/create because of H2 
drop table publisher.service;
create table publisher.service(
	id serial primary key,
	identification text,
	name text,
	title text,
	alternate_title text,
	abstract text,
	metadata text,
	generic_layer_id integer references publisher.generic_layer(id),
	constants_id integer references publisher.constants(id),
	-- add watermark
	watermark_enabled boolean default false,
	watermark_url text,
	watermark_transparency integer,
	watermark_position text,
	published boolean default false
);
 
alter table publisher.tiled_layer drop column identification;
alter table publisher.tiled_layer drop column mime_formats;

alter table publisher.constants drop column name;
alter table publisher.constants drop column url;

alter table publisher.style drop column format;
alter table publisher.style drop column version;

alter table publisher.leaf_layer drop column keywords;

--
-- Additional tables
--
-- table service keywords
create table publisher.service_keyword(
	keyword text,
	service_id integer references publisher.service(id)
);
-- table layer keywords
create table publisher.leaf_layer_keyword(
	keyword text,
	leaf_layer_id integer references publisher.leaf_layer(id)
);
-- table tiledlayer mimeformats
create table publisher.tiled_layer_mimeformat(
	mimeformat text,
	tiled_layer_id integer references publisher.tiled_layer(id)
);

insert into publisher.version(id) values(36);
-- -----------------------------------------
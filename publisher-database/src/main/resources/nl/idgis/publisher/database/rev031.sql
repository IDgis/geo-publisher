--
-- Drop old tables admin service configuration
--
drop table if exists publisher.jt_layerstyle cascade;
drop table if exists publisher.style cascade;
drop table if exists publisher.layer cascade;
drop table if exists publisher.jt_layergroup cascade;
drop table if exists publisher.layergroup cascade;
drop table if exists publisher.service cascade;
drop table if exists publisher.tiledlayer cascade;

-- 
-- Add new tables for admin service configuration
-- 
 
-- table constants  
create table publisher.constants(
	id serial primary key,
	identification text,
	name text,
	url text,
	contact text,
	organization text,
	position text,
	address_type text,
	address text,
	city text,
	state text,
	zipcode text,
	country text,
	telephone text,
	fax text,
	email text
);
 
-- table generic_layer  
create table publisher.generic_layer(
	id serial primary key,
	identification text,
	name text,
	title text,
	abstract text
);
 
-- table tiledlayer 
create table publisher.tiledlayer(
	id serial primary key,
	identification text,
	name text,
	enabled boolean default false,
	mimeformats text,
	metawidth integer,
	metaheight integer,
	expirecache integer,
	expireclients integer,
	gutter integer,
	generic_layer_id integer references publisher.generic_layer(id)
);
 
-- table leaf layer  
create table publisher.leaf_layer(
	id serial primary key,
	identification text,
	name text,
	title text,
	abstract text,
	keywords text,
	metadata text,
	filter text,
	generic_layer_id integer references publisher.generic_layer(id),
	dataset_id integer references publisher.dataset(id)
);
 
-- table style  
create table publisher.style(
	id serial primary key,
	identification text,
	name text,
	format text default 'SLD',
	version text default '1.0.0',
	definition text
);

-- join table layerstyle  
create table publisher.layer_style(
	id serial primary key,
	defaultstyle boolean default false,
	layer_id integer references publisher.leaf_layer(id),
	style_id integer references publisher.style(id)
);
 
-- join table layer_structure  
create table publisher.layer_structure(
	id serial primary key,
	parent_layer_id integer references publisher.generic_layer(id),
	child_layer_id integer references publisher.generic_layer(id),
	layerorder integer default 0
);
 
-- table service 
create table publisher.service(
	id serial primary key,
	identification text,
	name text,
	title text,
	alternatetitle text,
	abstract text,
	keywords text,
	metadata text,
	watermark text,
	rootgroup_id integer references publisher.generic_layer(id),
	defaultcategory_id integer references publisher.category(id),
	application_id integer references publisher.constants(id)
);

insert into publisher.version(id) values(31);
-- -------------------------------------------
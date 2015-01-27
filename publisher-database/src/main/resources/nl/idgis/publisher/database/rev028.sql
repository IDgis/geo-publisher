
-- 
-- Add domains for admin service configuration
-- 
 
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
	gutter integer
);
 
-- table group  
create table publisher.layergroup(
	id serial primary key,
	identification text,
	name text,
	title text,
	abstract text,
	parentgroup integer references publisher.layergroup(id),
	tiledlayer_id integer references publisher.tiledlayer(id)
);
 
-- table layer  
create table publisher.layer(
	id serial primary key,
	identification text,
	name text,
	title text,
	abstract text,
	keywords text,
	metadata text,
	tiledlayer_id integer references publisher.tiledlayer(id)
);
 
-- table style  
create table publisher.style(
	id serial primary key,
	identification text,
	name text,
	format text default 'SLD',
	version text default '1.0.0',
	metadata text,
	definition text
);

-- join table layerstyle  
create table publisher.jt_layerstyle(
	id serial primary key,
	defaultstyle boolean default false,
	layer_id integer references publisher.layer(id),
	style_id integer references publisher.style(id)
);
 
-- join table layergroup  
create table publisher.jt_layergroup(
	id serial primary key,
	layerorder integer default 0,
	group_id integer references publisher.layergroup(id),
	layer_id integer references publisher.layer(id),
	style_id integer references publisher.style(id)
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
	rootgroup_id integer references publisher.layergroup(id),
	defaultcategory_id integer references publisher.category(id)
);

insert into publisher.version(id) values(28);
-- -------------------------------------------
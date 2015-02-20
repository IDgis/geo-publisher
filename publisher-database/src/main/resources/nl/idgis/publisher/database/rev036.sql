--
-- Sevaral database changes  
--

--
-- Additional tables
--

-- table service keywords
create table publisher.service_keyword(
	keyword text,
	service_id integer references publisher.service(id),
);

-- table layer keywords
create table publisher.leaf_layer_keyword(
	keyword text,
	leaf_layer_id integer references publisher.leaf_layer(id),
);

-- table tiledlayer mimeformats
create table publisher.tiled_layer_mimeformat(
	mimeformat text,
	tiled_layer_id integer references publisher.tiled_layer(id),
);

--
-- changing columns (removing / renaming / adding)
--
alter table publisher.service drop column watermark;
alter table publisher.service drop column default_category_id;
-- rename rootgroup_id to generic_layer_id
alter table publisher.service drop column rootgroup_id;
alter table publisher.service add column generic_layer_id integer references generic_layer(id);

alter table publisher.tiled_layer drop column identification;
alter table publisher.tiled_layer drop column mime_formats;

alter table publisher.constants drop column name;
alter table publisher.constants drop column url;

alter table publisher.generic_layer add column style_id integer references style(id);

--
-- add foreign key references 
--


alter table publisher.service add constraint service_identification_key unique(identification);

alter table publisher.layer_structure add constraint layer_structure_style_id_fk foreign key (style_id) references publisher.style(id);



insert into publisher.version(id) values(36);
-- -----------------------------------------
-- 
-- several column renames, column remove
-- 

-- column rename
alter table publisher.service drop column defaultcategory_id;
alter table publisher.service add column default_category_id  references publisher.category(id);

alter table publisher.service drop column application_id;
alter table publisher.service add column constants_id integer references publisher.constants(id);

alter table publisher.layer_style drop column defaultstyle;
alter table publisher.layer_style add column default_style boolean default false;

alter table publisher.layer_structure drop column layerorder;
alter table publisher.layer_structure add column layer_order integer default 0;

-- column remove
alter table publisher.leaf_layer drop column identification;

-- add watermark
alter table publisher.service add column watermark_enabled boolean default false;
alter table publisher.service add column watermark_url text;
alter table publisher.service add column watermark_transparency integer;
alter table publisher.service add column watermark_position text;

insert into publisher.version(id) values(33);
-- ----------------------------------

-- 
-- tabel layer_style add order column for style_id 
-- 

ALTER TABLE publisher.layer_style ADD COLUMN style_order serial not null;

insert into publisher.version(id) values(52);

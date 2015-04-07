
-- 
-- give tabel layer_style a separate pk column for proper style_id order
-- the order was (layer_id, style_id) so that style order
-- was according to style.id and not the order in which it was saved
-- in the layer
-- 

ALTER TABLE publisher.layer_style DROP CONSTRAINT IF EXISTS layer_style_pkey;
ALTER TABLE publisher.layer_style ADD COLUMN id serial not null;

insert into publisher.version(id) values(52);

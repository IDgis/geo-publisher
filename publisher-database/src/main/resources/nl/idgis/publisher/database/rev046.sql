
--
-- change relation tiledlayer-tiledlayermimeformats to cascade delete
--
ALTER TABLE publisher.tiled_layer_mimeformat DROP CONSTRAINT IF EXISTS tiled_layer_mimeformat_tiled_layer_id_fkey;
ALTER TABLE publisher.tiled_layer_mimeformat ADD CONSTRAINT tiled_layer_mimeformat_tiled_layer_id_fkey FOREIGN KEY (tiled_layer_id) REFERENCES publisher.tiled_layer (id) ON DELETE CASCADE;

insert into publisher.version(id) values(46);

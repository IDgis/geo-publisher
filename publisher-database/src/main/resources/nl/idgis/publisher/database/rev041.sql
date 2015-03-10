--
-- delete constraint: cascade delete
--
ALTER TABLE publisher.layer_style DROP CONSTRAINT IF EXISTS layer_style_style_id_fkey;
ALTER TABLE publisher.layer_style ADD CONSTRAINT layer_style_style_id_fkey FOREIGN KEY (style_id) REFERENCES publisher.style (id);

insert into publisher.version(id) values(41);

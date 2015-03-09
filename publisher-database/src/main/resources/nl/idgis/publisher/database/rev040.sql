--
-- remove fields from service table and copy to generic_layer
--
-- copy to generic_layer

-- drop columns
-- identification
UPDATE publisher.generic_layer as g 
SET identification=(
	SELECT s.identification from publisher.service as s 
	join publisher.generic_layer as g2 on s.generic_layer_id = g2.id
	WHERE s.generic_layer_id=g.id
	)
	where exists 
	(
	SELECT s.identification from publisher.service as s 
	join publisher.generic_layer as g2 on s.generic_layer_id = g2.id
	WHERE s.generic_layer_id=g.id
	)
;

-- name
UPDATE publisher.generic_layer as g 
SET name=(
	SELECT s.name from publisher.service as s 
	join publisher.generic_layer as g2 on s.generic_layer_id = g2.id
	WHERE s.generic_layer_id=g.id
	)
	where exists 
	(
	SELECT s.name from publisher.service as s 
	join publisher.generic_layer as g2 on s.generic_layer_id = g2.id
	WHERE s.generic_layer_id=g.id
	)
;

-- abstract
UPDATE publisher.generic_layer as g 
SET abstract=(
	SELECT s.abstract from publisher.service as s 
	join publisher.generic_layer as g2 on s.generic_layer_id = g2.id
	WHERE s.generic_layer_id=g.id
	)
	where exists 
	(
	SELECT s.abstract from publisher.service as s 
	join publisher.generic_layer as g2 on s.generic_layer_id = g2.id
	WHERE s.generic_layer_id=g.id
	)
;

-- title
UPDATE publisher.generic_layer as g 
SET title=(
	SELECT s.title from publisher.service as s 
	join publisher.generic_layer as g2 on s.generic_layer_id = g2.id
	WHERE s.generic_layer_id=g.id
	)
	where exists 
	(
	SELECT s.title from publisher.service as s 
	join publisher.generic_layer as g2 on s.generic_layer_id = g2.id
	WHERE s.generic_layer_id=g.id
	)
;

-- published
UPDATE publisher.generic_layer as g 
SET published=(
	SELECT s.published from publisher.service as s 
	join publisher.generic_layer as g2 on s.generic_layer_id = g2.id
	WHERE s.generic_layer_id=g.id
	)
	where exists 
	(
	SELECT s.published from publisher.service as s 
	join publisher.generic_layer as g2 on s.generic_layer_id = g2.id
	WHERE s.generic_layer_id=g.id
	)
;


alter table publisher.service drop column identification;
alter table publisher.service drop column name;
alter table publisher.service drop column title;
alter table publisher.service drop column abstract;
alter table publisher.service drop column published;


insert into publisher.version(id) values(40);

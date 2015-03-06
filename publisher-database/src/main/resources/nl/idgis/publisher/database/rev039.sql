--
-- add 1 record to constants and update service constants_id
--
INSERT INTO publisher.constants (
    identification,
    contact ,
    organization ,
    "position" ,
    address_type ,
    address ,
    city ,
    state ,
    zipcode ,
    country ,
    telephone ,
    fax ,
    email 
) 
SELECT 'acb380a5-5270-4284-8fd1-b24f62809fe3', '', '', '', '', '', '', '', '', '', '', '', ''
  WHERE 
	NOT EXISTS (
        SELECT identification FROM publisher.constants 
    );
 ;
update publisher.service set constants_id = (select id from publisher.constants order by id desc limit 1);

insert into publisher.version(id) values(39);

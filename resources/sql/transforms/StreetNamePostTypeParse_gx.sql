-- StreetNamePostTypeParse_gx
CREATE TEMP TABLE post_type_temp as (
  SELECT distinct a.addressid, a.streetname, b.streettype,
     substr(a.streetname,1, instr(a.streetname,' ', -1)) as street2
    FROM
       %tablename%_core a,
       abbrmap b 
    WHERE
       (upper( substr(a.streetname, instr(a.streetname,' ', -1)+1,15)) = b.streettype or
       upper(substr(a.streetname, instr(a.streetname,' ', -1)+1,15)) = b.abbr ) and
       (b.filter is null and
	b.istype = '1' and
       a.streetname not like '% & %' )
);
DELETE FROM post_type_temp where streettype is null;
UPDATE %tablename%_core set
  streetname = (SELECT p.street2 
        FROM post_type_temp p 
	WHERE p.addressid = %tablename%_core.addressid),
  streetnameposttype = (SELECT p.streettype FROM post_type_temp p 
	WHERE p.addressid = %tablename%_core.addressid)
  WHERE
    EXISTS (select addressid from post_type_temp p 
	WHERE p.addressid = %tablename%_core.addressid)
;


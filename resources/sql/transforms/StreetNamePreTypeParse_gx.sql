-- StreetNamePreTypeParse_gx
CREATE TEMP TABLE pre_type_temp as (
  SELECT distinct a.addressid, a.streetname, b.streettype,
     substr(a.streetname,instr(a.streetname,' ', 1)+1,80) as street2
    FROM
       %tablename%_core a,
       abbrmap b 
    WHERE
       ( upper(substr(a.streetname, 1, instr(a.streetname,' ', 1)-1)) = b.streettype or
       upper(substr(a.streetname, 1, instr(a.streetname,' ', 1)-1)) = b.abbr ) and
       (b.filter is null and
       b.istype = '1' and
       instr(a.streetname, ' & ') = 0 )
);
DELETE FROM pre_type_temp where streettype is null;
UPDATE %tablename%_core set
  streetname = (SELECT p.street2 
        FROM pre_type_temp p 
	WHERE p.addressid = %tablename%_core.addressid),
  streetnamepretype = (SELECT p.streettype FROM pre_type_temp p 
	WHERE p.addressid = %tablename%_core.addressid)
  WHERE
    EXISTS (select addressid from pre_type_temp p 
	WHERE p.addressid = %tablename%_core.addressid)
;


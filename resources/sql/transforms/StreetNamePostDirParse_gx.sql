-- StreetNamePostTypeParse_gx
CREATE TEMP TABLE post_dir_temp as (
  SELECT a.addressid, a.streetname, b.streettype as streetdir,
     substr(a.streetname,1, instr(a.streetname,' ', -1)) as street2
    FROM
       %tablename%_core a,
       abbrmap b 
    WHERE
       ( upper(substr(a.streetname, instr(a.streetname,' ', -1)+1,15)) = b.streettype or
       upper(substr(a.streetname, instr(a.streetname,' ', -1)+1,15)) = b.abbr ) and
       (b.filter is null and
	b.isdir = '1' and
       a.streetname not like '% & %' )
);
DELETE FROM post_dir_temp where streetdir is null;
UPDATE %tablename%_core set
  streetname = (SELECT p.street2 
        FROM post_dir_temp p 
	WHERE p.addressid = %tablename%_core.addressid),
  streetnamepostdirectional = (SELECT p.streetdir FROM post_dir_temp p 
	WHERE p.addressid = %tablename%_core.addressid)
  WHERE
    EXISTS (select addressid from post_dir_temp p 
	WHERE p.addressid = %tablename%_core.addressid)
;


-- StreetNamePreDirParse_gx
CREATE TEMP TABLE pre_dir_temp as (
  SELECT a.addressid, a.streetname, b.streettype as streetdir,
     substr(a.streetname,instr(a.streetname,' ', 1)+1,80) as street2
    FROM
       %tablename%_core a,
       abbrmap b 
    WHERE
       ( upper(substr(a.streetname, 1, instr(a.streetname,' ', 1)-1)) = b.streettype or
       upper(substr(a.streetname, 1, instr(a.streetname,' ', 1)-1)) = b.abbr ) and
       (b.filter is null and
       b.isdir = '1' and
       instr(a.streetname, ' & ') = 0 )
);
DELETE FROM pre_dir_temp where streetdir is null;
UPDATE %tablename%_core set
  streetname = (SELECT p.street2 
        FROM pre_dir_temp p 
	WHERE p.addressid = %tablename%_core.addressid),
  streetnamepredirectional = (SELECT p.streetdir FROM pre_dir_temp p 
	WHERE p.addressid = %tablename%_core.addressid)
  WHERE
    EXISTS (select addressid from pre_dir_temp p 
	WHERE p.addressid = %tablename%_core.addressid)
;


-- StreetNamePostAbbrev_gx
UPDATE %tablename%_core set
  streetnameposttype = coalesce((SELECT a.streettype 
                          FROM abbrmap a
                          WHERE a.abbr = %tablename%_core.streetnameposttype and
                          a.filter is null), streetnameposttype, '')
;


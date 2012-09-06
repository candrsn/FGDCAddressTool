-- StreetNamePostDirAbbrev_gx
UPDATE %tablename%_core set
  streetnamepostdirectional = coalesce((SELECT a.streettype
                          FROM abbrmap a
                          WHERE a.abbr = %tablename%_core.streetnamepostdirectional and
                          a.filter is null), %tablename%_core.streetnamepostdirectional,'')
;


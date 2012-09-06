-- StreetNamePreDirAbbrev_gx
UPDATE %tablename%_core set
  streetnamepredirectional = coalesce((SELECT a.streettype
                          FROM abbrmap a
                          WHERE a.abbr = %tablename%_core.streetnamepredirectional and
                          a.filter is null), %tablename%_core.streetnamepredirectional,'')
;


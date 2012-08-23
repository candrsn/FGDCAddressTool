-- StreetNamePreAbbrev_gx
UPDATE %tablename%_core set
  streetnamepretype = coalesce((SELECT a.streettype
                          FROM abbrmap a
                          WHERE a.abbr = %tablename%_core.streetnamepretype and
                          a.filter is null), %tablename%_core.streetnamepretype,'')
;


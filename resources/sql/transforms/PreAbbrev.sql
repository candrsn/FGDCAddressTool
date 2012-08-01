UPDATE %tablename%_core set
  streetnamepretype = (SELECT coalesce(a.streettype, %tablename%_core.streetnamepretype)
                          FROM abbrmap a
                          WHERE a.abbr = %tablename%_core.streetnamepretype and
                          a.filter is null)
;


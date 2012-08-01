UPDATE %tablename%_core set
  streetnameposttype = (SELECT coalesce(a.streettype, %tablename%_core.streetnameposttype) 
                          FROM abbrmap a
                          WHERE a.abbr = %tablename%_core.streetnameposttype and
                          a.filter is null)
;


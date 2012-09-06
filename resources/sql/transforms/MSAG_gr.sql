-- MSAG_gr
  SELECT
      a.addressid,
      regexp_replace(
      a.streetnamepremodifier || ' '||
      a.streetnamepredirectional || ' ' ||
      a.streetnamepretype || ' ' ||
      a.streetname || ' ' ||   
      a.streetnameposttype || ' ' ||
      a.streetnamepostdirectional || ' ' ||
      a.streetnamepostmodifier ,'  +',' ')
       as CompleteStreetName,
      a.streetnamepremodifier,
      a.streetnamepredirectional,
      a.streetnamepretype,
      a.streetname,
      a.streetnameposttype,
      a.streetnamepostdirectional,
      a.streetnamepostmodifier,
      b.streetnamepremodifier as matchpremodifier,
      b.streetnamepredirectional as matchpredirectional,
      b.streetnamepretype as matchpretype,
      b.streetname as matchname,
      b.streetnameposttype as matchposttype,
      b.streetnamepostdirectional as matchpostdirectional,
      b.streetnamepostmodifier as matchpostmodifier,
      a.%zonefield% as matchzone
    FROM       
       %tablename%_core a LEFT JOIN
       %tablename%_prelim b on (a.addressid = b.addressid) 
;


-- UniqueStreetAddressMeasure
--ReportNothing 
-- Tested StreetAddress using UniquenessMeasure at 100% conformance.
--Setup
CREATE TEMP TABLE QA_tmp as (
  SELECT
      a.addressid,
      a.addressnumberprefix || ' ' ||
      a.addressnumber || ' ' ||
      a.addressnumbersuffix || ' ' ||
      a.streetnamepremodifier || ' '||
      a.streetnamepredirectional || ' ' ||
      a.streetnamepretype || ' ' ||
      a.streetname || ' ' ||   
      a.streetnameposttype || ' ' ||
      a.streetnamepostdirectional || ' ' ||
      a.streetnamepostmodifier || ' ' ||
      coalesce(o.subaddressid,'')
       as element
    FROM       
       %tablename%_core a LEFT OUTER JOIN
       %tablename%_subaddress o on (a.addressid = o.addressid and o.subaddressorder = 1)
);
CREATE INDEX qa_tmp__element__ind on qa_tmp(element);

--Query
SELECT
   'UniqueStreetAddressMeasure: '|| element || ', ' || cnt || ' repeated times' 
 from 
  (select 
    COUNT(*) as cnt, 
      element
    FROM
       qa_tmp
    GROUP BY
      element
    HAVING
       COUNT(*) > 1
  ) as r
;
  
--Cleanup
DROP TABLE QA_tmp;



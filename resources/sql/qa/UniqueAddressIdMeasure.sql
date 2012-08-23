-- UniqueAddressIdMeasure
--ReportNothing 
-- Tested AddressId using UniquenessMeasure at 100% conformance.

--Query
SELECT
   'UniqueAddressIdMeasure: '|| element || ', ' || cnt || ' repeated times' 
 from 
  (select 
    COUNT(*) as cnt, 
      a.addressid as element
    FROM
       %tablename%_core a
    GROUP BY
      element
    HAVING
       COUNT(*) > 1
  ) as r
  

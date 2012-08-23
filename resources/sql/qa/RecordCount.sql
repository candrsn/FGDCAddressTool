SELECT 'The number of address entries is '||cnt
  FROM
    (SELECT COUNT(*) as cnt
       FROM %tablename%_core
    );

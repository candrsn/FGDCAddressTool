SELECT 'The Number of entries is '||cnt
  FROM
    (SELECT COUNT(*) as cnt
       FROM address_core
    );

-- NoNulls.sql

UPDATE %tablename%_core set
addressid                      = coalesce(addressid  ,''),
addressauthority               = coalesce(addressauthority ,''),

completeaddressnumber          = coalesce(completeaddressnumber ,''),
addressnumberprefix            = coalesce(addressnumberprefix ,''),
addressnumberprefix_sep        = coalesce(addressnumberprefix_sep ,''),
addressnumber                  = coalesce(addressnumber ,''),
addressnumbersuffix            = coalesce(addressnumbersuffix ,''),
addressnumbersuffix_sep        = coalesce(addressnumbersuffix_sep ,''),

streetnamepremodifier          = coalesce(streetnamepremodifier ,''),
streetnamepremodifier_sep      = coalesce(streetnamepremodifier_sep ,''),
streetnamepredirectional       = coalesce(streetnamepredirectional ,''),
streetnamepredirectional_sep   = coalesce(streetnamepredirectional_sep ,''),
streetnamepretype              = coalesce(streetnamepretype ,''),
streetnamepretype_sep          = coalesce(streetnamepretype_sep ,''),
streetname                     = coalesce(streetname ,''),
streetnameposttype             = coalesce(streetnameposttype ,''),
streetnameposttype_sep         = coalesce(streetnameposttype_sep ,''),
streetnamepostdirectional      = coalesce(streetnamepostdirectional ,''),
streetnamepostdirectional_sep  = coalesce(streetnamepostdirectional_sep ,''),
streetnamepostmodifier         = coalesce(streetnamepostmodifier ,''),
streetnamepostmodifier_sep     = coalesce(streetnamepostmodifier_sep ,''),

statename                      = coalesce(statename ,''),
zipcode                        = coalesce(zipcode ,''),
zipplus4                       = coalesce(zipplus4  ,''),

countryname                    = coalesce(countryname  ,''),
placestatezip                  = coalesce(placestatezip ,''),

addressxcoordinate             = coalesce(addressxcoordinate ,''),
addressycoordinate             = coalesce(addressycoordinate ,''),
addresslongitude               = coalesce(addresslongitude ,''),
addresslatitude                = coalesce(addresslatitude ,''),

usgsnationalgridcoordinate     = coalesce(usgsnationalgridcoordinate ,''),
addresselevation               = coalesce(addresselevation ,''),

addresscordinatereferencesystemid               = coalesce(addresscordinatereferencesystemid ,''),
addresscordinatereferencesystemauthority        = coalesce(addresscordinatereferencesystemauthority ,''),

addressparcelidentifier        = coalesce(addressparcelidentifier ,''),
addressparcelauthority         = coalesce(addressparcelauthority ,''),
addresstransportationsystemname                 = coalesce(addresstransportationsystemname ,''),
addresstransportationsystemauthority            = coalesce(addresstransportationsystemauthority ,''),

addressclassification          = coalesce(addressclassification ,''),
addressfeaturetype              = coalesce(addressfeaturetype ,''),
addresslifecyclestatus         = coalesce(addresslifecyclestatus ,''),

officialstatus                 = coalesce(officialstatus ,''),
addressanomalystatus           = coalesce(addressanomalystatus ,''),
addresssideofstreet            = coalesce(addresssideofstreet ,''),
addresszlevel                  = coalesce(addresszlevel ,''),
locationdescription            = coalesce(locationdescription ,''),
mailableaddress                = coalesce(mailableaddress ,''),
addressstartdate                = coalesce(addressstartdate ,''),
addressenddate                 = coalesce(addressenddate ,''),
datasetid                      = coalesce(datasetid ,''),
addressreferencesystemid       = coalesce(addressreferencesystemid ,''),
addressreferencesystemauthority                = coalesce(addressreferencesystemauthority ,'')
;


DELETE FROM %tablename%_place
  WHERE placename is null or placename = '' or
    addressid is null or addressid = '';
UPDATE %tablename%_place set
addressid          = coalesce(addressid ,''),
placename          = coalesce(placename ,''),
placenametype      = coalesce(placenametype ,''),
placenameorder     = coalesce(placenameorder,1)
;


DELETE FROM %tablename%_subaddress
  WHERE addressid is null or addressid = '' or
    subaddressid is null or subaddressid = '';
UPDATE %tablename%_subaddress set
addressid          = coalesce(addressid ,''),
subaddresstype     = coalesce(subaddresstype ,''),
subaddressid       = coalesce(subaddressid ,''),
subaddressorder    = coalesce(subaddressorder,1)
;


UPDATE %tablename%_relation set
addressid          = coalesce(addressid ,''),
relatedaddressid   = coalesce(relatedaddressid ,''),
relationrole       = coalesce(relationrole ,''),
relationstatus     = coalesce(relationstatus ,'')
;


UPDATE %tablename%_QA set
addressid          = coalesce(addressid ,''),
qa_metric          = coalesce(qa_metric ,''),
qa_value           = coalesce(qa_value , ''),
qa_date            = coalesce(qa_date ,now()),
qa_operator        = coalesce(qa_operator , user())
;

-- address_table_tmpl.sql
-- these tables have loose datatyping to facilitate data parsing and modification


CREATE TABLE %tablename%_core (
addressid                      varchar(80),
addressauthority               varchar(160),

-- completelandmarkname *
-- completeplacename *

completeaddressnumber          varchar(80),
addressnumberprefix            varchar(80),
addressnumberprefix_sep        varchar(80),
addressnumber                  varchar(80),
addressnumbersuffix            varchar(80),
addressnumbersuffix_sep        varchar(80),

-- completestreetname
streetnamepremodifier          varchar(80),
streetnamepremodifier_sep      varchar(80),
streetnamepredirectional       varchar(80),
streetnamepredirectional_sep   varchar(80),
streetnamepretype              varchar(80),
streetnamepretype_sep          varchar(80),
streetname                     varchar(80),
streetnameposttype             varchar(80),
streetnameposttype_sep         varchar(80),
streetnamepostdirectional      varchar(80),
streetnamepostdirectional_sep  varchar(80),
streetnamepostmodifier         varchar(80),
streetnamepostmodifier_sep     varchar(80),

-- completesubaddress *
-- completeplacename *

statename                      varchar(80),
zipcode                        varchar(5),
zipplus4                       varchar(4),

countryname                    varchar(45),
placestatezip                  varchar(80),

-- relatedaddressid *

addressxcoordinate             varchar(20),
addressycoordinate             varchar(20),
addresslongitude               varchar(20),
addresslatitude                varchar(20),

usgsnationalgridcoordinate     varchar(40),
addresselevation               varchar(15),

addresscordinatereferencesystemid               varchar(15),
addresscordinatereferencesystemauthority        varchar(15),

addressparcelidentifier        varchar(160),
addressparcelauthority         varchar(160),
addresstransportationsystemname                 varchar(160),
addresstransportationsystemauthority            varchar(160),

addressclassification          varchar(160),
adressfeaturetype              varchar(160),
addresslifecyclestatus         varchar(160),

officialstatus                 varchar(160),
addressanomalyststus           varchar(160),
addresssideofstreet            varchar(160),
addresszlevel                  varchar(160),
locationdescription            varchar(260),
mailableaddress                varchar(160),
addresstartdate                varchar(160),
addressenddate                 varchar(160),
datasetid                      varchar(160),
addressreferencesystemid       varchar(160),
addressreferencesystemauthority                varchar(160)
);



CREATE TABLE %tablename%_prelim (
addressid                      varchar(80),
addressauthority               varchar(160),

-- completelandmarkname *
-- completeplacename *

completeaddressnumber          varchar(80),
addressnumberprefix            varchar(80),
addressnumberprefix_sep        varchar(80),
addressnumber                  varchar(80),
addressnumbersuffix            varchar(80),
addressnumbersuffix_sep        varchar(80),

-- completestreetname
streetnamepremodifier          varchar(80),
streetnamepremodifier_sep      varchar(80),
streetnamepredirectional       varchar(80),
streetnamepredirectional_sep   varchar(80),
streetnamepretype              varchar(80),
streetnamepretype_sep          varchar(80),
streetname                     varchar(80),
streetnameposttype             varchar(80),
streetnameposttype_sep         varchar(80),
streetnamepostdirectional      varchar(80),
streetnamepostdirectional_sep  varchar(80),
streetnamepostmodifier         varchar(80),
streetnamepostmodifier_sep     varchar(80),

-- completesubaddress *
-- completeplacename *

statename                      varchar(80),
zipcode                        varchar(5),
zipplus4                       varchar(4),

countryname                    varchar(45),
placestatezip                  varchar(80),

-- relatedaddressid *

addressxcoordinate             varchar(20),
addressycoordinate             varchar(20),
addresslongitude               varchar(20),
addresslatitude                varchar(20),

usgsnationalgridcoordinate     varchar(40),
addresselevation               varchar(15),

addresscordinatereferencesystemid               varchar(15),
addresscordinatereferencesystemauthority        varchar(15),

addressparcelidentifier        varchar(160),
addressparcelauthority         varchar(160),
addresstransportationsystemname                 varchar(160),
addresstransportationsystemauthority            varchar(160),

addressclassification          varchar(160),
adressfeaturetype              varchar(160),
addresslifecyclestatus         varchar(160),

officialstatus                 varchar(160),
addressanomalyststus           varchar(160),
addresssideofstreet            varchar(160),
addresszlevel                  varchar(160),
locationdescription            varchar(260),
mailableaddress                varchar(160),
addresstartdate                varchar(160),
addressenddate                 varchar(160),
datasetid                      varchar(160),
addressreferencesystemid       varchar(160),
addressreferencesystemauthority                varchar(160)
);



CREATE TABLE %tablename%_place (
addressid          varchar(60),
placename          varchar(160),
placenametype      varchar(25),
placenameorder     integer
);


CREATE TABLE %tablename%_subaddress (
addressid          varchar(60),
subaddresstype     varchar(60),
subaddressid       varchar(60),
subaddressorder    integer
);


CREATE TABLE %tablename%_relation (
addressid          varchar(60),
relatedaddressid   varchar(60),
relationrole       varchar(25),
relationstatus     varchar(25)
);


CREATE TABLE %tablename%_QA (
addressid          varchar(60),
qa_metric          varchar(160),
qa_value           varchar(160),
qa_date            varchar(160),
qa_operator        varchar(160)
);



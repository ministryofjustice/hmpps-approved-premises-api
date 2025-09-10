update characteristics set property_name = 'notSuitableForArsonOffenders' where name = 'Not suitable for arson offenders' and service_scope = 'temporary-accommodation' and property_name is null and is_active is false;
update characteristics set property_name = 'hasLiftAccess' where name = 'Lift access'  and service_scope = 'temporary-accommodation' and property_name is null and is_active is false;
update characteristics set property_name = 'singleBed' where name = 'Single bed' and service_scope = 'temporary-accommodation' and property_name is null and is_active is false;
update characteristics set property_name = 'hasSingleBed' where name = 'Single bed' and service_scope = 'temporary-accommodation' and property_name is null and is_active is false;
update characteristics set property_name = 'hasDoubleBed' where name = 'Double bed' and service_scope = 'temporary-accommodation' and property_name is null and is_active is false;
update characteristics set property_name = 'notSuitableForRegisteredSexOffenders(RSO)' where name = 'Not suitable for registered sex offenders (RSO)' and service_scope = 'temporary-accommodation' and property_name is null and is_active is false;


select
-- get count of total bedspace voids
(select count(vb.*)
 from cas3_void_bedspaces vb)             as void_bedspaces,
-- get count of existing/old style cancellations
(select count(c.*)
 from cas3_void_bedspace_cancellations c) as void_bedspace_cancellations,
-- get count of migrated cancellations
(select count(vb.*)
 from cas3_void_bedspaces vb
 where vb.bedspace_id is not null)  as migrated_voids,

-- get count of non-migrated bedspaces
(select count(vb.*)
 from cas3_void_bedspaces vb
 where vb.cancellation_date is not null)      as migrated_cancellations)      as migrated_cancellations
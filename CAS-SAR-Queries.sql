-- queries for SAR - all have params specifying:
-- CRN or NOMIS NUMBER, optionally 
-- created dates between a start date and end date

	
-- approved premise applications
-- DONE SEE JPA CODE
	
-- applications timeline notes (AP only)
-- DONE SEE JPA CODE

-- AP assessments
-- DONE SEE JPA CODE

-- assessment clarification notes ap
-- DONE SEE JPA CODE

-- assessment referral history notes -approved premises
-- DONE SEE JPA CODE

-- bookings - Approved Premises
-- DONE SEE JPA CODE

-- booking extensions -AP
-- DONE SEE JPA CODE

-- cancellations - AP
-- DONE SEE JPA CODE

-- bed moves - AP
-- moved to JPA
select
    b.crn ,
    b.noms_number,
    bm.notes,
    previous_bed."name" as previous_bed_name,
    previous_bed.code as previous_bed_code,
    new_bed."name" as new_bed_name,
    new_bed.code as new_bed_code,
    bm.created_at
from
    bed_moves bm
        inner join bookings b on
        b.id = bm.booking_id
        inner join beds previous_bed on
        bm.previous_bed_id  = previous_bed.id
        inner join beds new_bed on
        bm.new_bed_id  = new_bed.id

where
    b.service = :service_name and
        (b.crn = :crn
            or b.noms_number = :noms_number )
      and (:start_date is null or b.created_at >= :start_date)
      and (:end_date is null or b.created_at <= :end_date);

-- appeals - AP
select
    app.crn,
    app.noms_number,
    a.appeal_date,
    a.appeal_detail,
    a.decision ,
    a.decision_detail,
    a.created_at as appeal_created_at,
    u."name" as user_name
from
    appeals a
        inner join users u on
        u.id = a.created_by_user_id
        left join applications app on
        app.id = a.application_id
        left join assessments assess on
        assess.application_id = app.id
where
    app.service = 'approved-premises' and
    (app.crn = :crn
        or app.noms_number = :noms_number )
  and (:start_date is null or app.created_at >= :start_date)
  and (:end_date is null or app.created_at <= :end_date);


-- placement requests - AP
select
    a.crn,
    a.noms_number,
    pr.created_at,
    pr.expected_arrival,
    pr.duration,
    au."name" as allocated_user,
    pr.is_parole,
    pr.reallocated_at,
    pr.is_withdrawn,
    pr.withdrawal_reason,
    pr.notes,
    pr.due_at,
    case
        when preq.ap_type = '0' then 'NORMAL'
        when preq.ap_type = '1' then 'PIPE'
        when preq.ap_type = '2' then 'ESAP'
        when preq.ap_type = '3' then 'RFAP'
        when preq.ap_type = '4' then 'MHAP_ST_JOSEPHS'
        when preq.ap_type = '5' then 'MHAP_ELLIOTT_HOUSE'
        else 'other'
        end ap_type,
    preq.gender,
    pd.outcode
from
    placement_requests pr
        join placement_applications pa on
        pa.id = pr.placement_application_id
        join applications a on
        a.id = pa.application_id
        left join users au on
        au.id = pr.allocated_to_user_id
        left join placement_requirements preq on
        preq.id = pr.placement_requirements_id
        left join postcode_districts pd on
        pd.id = preq.postcode_district_id
where
    a.service = 'approved-premises' and
    (a.crn = :crn
        or a.noms_number = :noms_number )
  and (:start_date is null or a.created_at >= :start_date)
  and (:end_date is null or a.created_at <= :end_date);

-- placements applications - Approved Premises
select
    a.crn ,
    a.noms_number,
    pa."data" ,
    pa."document",
    pa.created_at,
    pa.submitted_at ,
    pa.allocated_at,
    pa.reallocated_at,
    pa.due_at ,
    pa.decision,
    pa.decision_made_at,
    pa.placement_type,
    pa.withdrawal_reason,
    cu."name" as created_by_user,
    au."name" as allocated_user
from
    placement_applications pa
        join applications a on
        pa.application_id = a.id
        join users cu on
        cu.id = pa.created_by_user_id
        left join users au on
        au.id = pa.allocated_to_user_id
where
    a.service = 'approved-premises' and
    (a.crn = :crn
        or a.noms_number = :noms_number )
  and (:start_date is null or a.created_at >= :start_date)
  and (:end_date is null or a.created_at <= :end_date);

-- Domain events - Approved Premises
select
    de.crn ,
    de.noms_number,
    de.occurred_at,
    de."data" ,
    de."type",
    de.created_at,
    de.service ,
    coalesce(u."name", 'unknown') as triggered_by
from
    domain_events de
        left join users u on u.id = de.triggered_by_user_id
where
    de.service = 'approved-premises' and
    (de.crn = :crn
        or de.noms_number = :noms_number )
  and (:start_date is null or de.created_at >= :start_date)
  and (:end_date is null or de.created_at <= :end_date);


-- ****CAS3****
-- temporary accommodation specific applications -REVIEW CORE APPLICATION FIELDS
select
	a.service,
	a.crn,
	a.noms_number,
	a."data",
	a."document",
	a.created_at,
	a.submitted_at,
	u."name" as applications_user_name ,
	conviction_id,
	event_number,
	offence_id,
	probation_region_id,
	risk_ratings,
	arrival_date,
	is_registered_sex_offender,
	needs_accessible_property,
	has_history_of_arson,
	is_duty_to_refer_submitted,
	duty_to_refer_submission_date,
	is_eligible,
	eligibility_reason,
	duty_to_refer_local_authority_area_name,
	prison_name_on_creation,
	person_release_date,
	pdu,
	is_history_of_sexual_offence,
	is_concerning_sexual_behaviour,
	is_concerning_arson_behaviour,
	duty_to_refer_outcome
from
	temporary_accommodation_applications taa
inner join applications a on
	a.id = taa.id
join users u on
	u.id = a.created_by_user_id
where 
	(a.crn = :crn
		or a.noms_number = :noms_number )
	and (:start_date is null or a.created_at >= :start_date) 
	and (:end_date is null or a.created_at <= :end_date);
	
-- temporary accommodation assessments - ADD Fields from core assessment table.
select
	app.crn,
	app.noms_number,
	taa.assessment_id as assessment_id,
	taa.summary_data,
	taa.completed_at,
	taa.referral_rejection_reason_detail,
	rrr."name" as referral_rejection_reason_category
from
	temporary_accommodation_assessments taa
join assessments assess on
	assess.id = taa.assessment_id
join applications app on
	app.id = assess.application_id
left join referral_rejection_reasons rrr on
	rrr.id = taa.referral_rejection_reason_id
where 
	(app.crn = :crn
		or app.noms_number = :noms_number )
	and (:start_date is null or app.created_at >= :start_date) 
	and (:end_date is null or app.created_at <= :end_date);
	
-- assessment referral history notes - temporary accommodation REVIEW Identifiers
select
	app.crn,
	app.noms_number,
	arhn.message,
	arhn.created_at
from
	assessment_referral_history_notes arhn
join assessments assess on
	assess.id = arhn.assessment_id
join applications app on
	app.id = assess.application_id
where
	assess.service = 'temporary-accommodation'
	and 
		(app.crn = :crn
		or app.noms_number = :noms_number )
	and (:start_date is null or app.created_at >= :start_date) 
	and (:end_date is null or app.created_at <= :end_date);


-- bookings - Temporary Accommodation
select
    b.crn ,
    b.noms_number,
    b.arrival_date,
    b.departure_date,
    b.original_arrival_date,
    b.original_departure_date,
    b.created_at,
    b.status,
    p."name" as premises_name,
    b.adhoc,
    b.key_worker_staff_code,
    b.service
from
    bookings b
        left join premises p on
        b.premises_id = p.id
where
    b.service = 'temporary-accommodation'
  and
    (b.crn = :crn
        or b.noms_number = :noms_number )
  and (:start_date is null or b.created_at >= :start_date)
  and (:end_date is null or b.created_at <= :end_date);

-- booking extensions -Temporary accommodation
select
    b.crn,
    b.noms_number,
    e.previous_departure_date,
    e.new_departure_date,
    e.notes,
    e.created_at
from
    extensions e
        join bookings b on
        b.id = e.booking_id
        left join applications a on
        a.id = b.application_id
        left join offline_applications oa on
        oa.id = b.offline_application_id
where
    b.service = 'temporary-accommodation' and
    (b.crn = :crn
        or b.noms_number = :noms_number )
  and (:start_date is null or b.created_at >= :start_date)
  and (:end_date is null or b.created_at <= :end_date);

-- cancellations - Temporary accommodation
select
    b.crn,
    b.noms_number,
    c.notes,
    c."date",
    cr."name" as cancellation_reason,
    c.other_reason,
    c.created_at
from
    cancellations c
        inner join bookings b on
        b.id = c.booking_id
        inner join cancellation_reasons cr on
        c.cancellation_reason_id = cr.id
where
    b.service = 'temporary-accommodation' and
    (b.crn = :crn
        or b.noms_number = :noms_number )
  and (:start_date is null or b.created_at >= :start_date)
  and (:end_date is null or b.created_at <= :end_date);
-- appeals - temporary accommodation
select
    app.crn,
    app.noms_number,
    a.appeal_date,
    a.appeal_detail,
    a.decision ,
    a.decision_detail,
    a.created_at as appeal_created_at,
    u."name" as user_name
from
    appeals a
        inner join users u on
        u.id = a.created_by_user_id
        left join applications app on
        app.id = a.application_id
        left join assessments assess on
        assess.application_id = app.id
where
    app.service = 'temporary-accommodation' and
    (app.crn = :crn
        or app.noms_number = :noms_number )
  and (:start_date is null or app.created_at >= :start_date)
  and (:end_date is null or app.created_at <= :end_date);

-- bed moves - Temporary accommodation
select
    b.crn ,
    b.noms_number,
    bm.notes,
    bm.created_at
from
    bed_moves bm
        inner join bookings b on
        b.id = bm.booking_id
where
    b.service = 'temporary-accommodation' and
    (b.crn = :crn
        or b.noms_number = :noms_number )
  and (:start_date is null or b.created_at >= :start_date)
  and (:end_date is null or b.created_at <= :end_date);

-- placement requests - Temporary accommodation
select
    a.crn,
    a.noms_number,
    pr.created_at,
    pr.expected_arrival,
    pr.duration,
    au."name" as allocated_user,
    pr.is_parole,
    pr.reallocated_at,
    pr.is_withdrawn,
    pr.withdrawal_reason,
    pr.notes,
    pr.due_at,
    case
        when preq.ap_type = '0' then 'NORMAL'
        when preq.ap_type = '1' then 'PIPE'
        when preq.ap_type = '2' then 'ESAP'
        when preq.ap_type = '3' then 'RFAP'
        when preq.ap_type = '4' then 'MHAP_ST_JOSEPHS'
        when preq.ap_type = '5' then 'MHAP_ELLIOTT_HOUSE'
        else 'other'
        end ap_type,
    preq.gender,
    pd.outcode
from
    placement_requests pr
        join placement_applications pa on
        pa.id = pr.placement_application_id
        join applications a on
        a.id = pa.application_id
        left join users au on
        au.id = pr.allocated_to_user_id
        left join placement_requirements preq on
        preq.id = pr.placement_requirements_id
        left join postcode_districts pd on
        pd.id = preq.postcode_district_id
where
    a.service = 'temporary-accommodation' and
    (a.crn = :crn
        or a.noms_number = :noms_number )
  and (:start_date is null or a.created_at >= :start_date)
  and (:end_date is null or a.created_at <= :end_date);

-- placements applications - Temporary Accommodation
select
    a.crn ,
    a.noms_number,
    pa."data" ,
    pa."document",
    pa.created_at,
    pa.submitted_at ,
    pa.allocated_at,
    pa.reallocated_at,
    pa.due_at ,
    pa.decision,
    pa.decision_made_at,
    pa.placement_type,
    pa.withdrawal_reason,
    cu."name" as created_by_user,
    au."name" as allocated_user
from
    placement_applications pa
        join applications a on
        pa.application_id = a.id
        join users cu on
        cu.id = pa.created_by_user_id
        left join users au on
        au.id = pa.allocated_to_user_id
where
    a.service = 'temporary-accommodation' and
    (a.crn = :crn
        or a.noms_number = :noms_number )
  and (:start_date is null or a.created_at >= :start_date)
  and (:end_date is null or a.created_at <= :end_date);

-- domain events - Temporary accommodation
select
    de.crn ,
    de.noms_number,
    de.occurred_at,
    de."data" ,
    de."type",
    de.created_at,
    de.service ,
    coalesce(u."name", 'unknown') as triggered_by
from
    domain_events de
        left join users u on u.id = de.triggered_by_user_id
where
    de.service = 'temporary accommodation' and
    (de.crn = :crn
        or de.noms_number = :noms_number )
  and (:start_date is null or de.created_at >= :start_date)
  and (:end_date is null or de.created_at <= :end_date);


-- ****CAS2****
-- cas2 applications
select
	ca.crn,
	ca.noms_number,
	ca."data",
	ca."document",
	ca.created_at,
	ca.submitted_at,
	ca.referring_prison_code,
	ca.preferred_areas,
	ca.telephone_number,
	ca.hdc_eligibility_date,
	ca.conditional_release_date,
	nu."name" as created_by_user
from
	cas_2_applications ca
inner join nomis_users nu on
	nu.id = ca.created_by_user_id
where 
	(ca.crn = :crn
		or ca.noms_number = :noms_number ) 
	and (:start_date is null or ca.created_at >= :start_date) 
	and (:end_date is null or ca.created_at <= :end_date)
	
-- cas2 assessments
select
	ca.crn,
	ca.noms_number,
	caa.created_at,
	caa.assessor_name,
	caa.nacro_referral_id
from
	cas_2_assessments caa
inner join cas_2_applications ca 
on
	ca.id = caa.application_id
where 
		(ca.crn = :crn
		or ca.noms_number = :noms_number )
	and (:start_date is null or ca.created_at >= :start_date) 
	and (:end_date is null or ca.created_at <= :end_date);

-- cas2 status updates
select
	ca.crn,
	ca.noms_number,
	csu.description,
	csu."label",
	csu.created_at
from 
	cas_2_status_updates csu
inner join cas_2_applications ca on
	ca.id = csu.application_id
where  
		(ca.crn = :crn
		or ca.noms_number = :noms_number )
	and (:start_date is null or ca.created_at >= :start_date) 
	and (:end_date is null or ca.created_at <= :end_date);

-- CAS2 status update details
select
	ca.crn,
	ca.noms_number,
	csu."label" as update_label,
	csu.description as update_description,
	csud."label" ,
	csud.created_at
from 
	cas_2_status_update_details csud
inner join 
	cas_2_status_updates csu 
on 
	csu.id = csud.status_update_id
inner join 
	cas_2_applications ca on
	ca.id = csu.application_id
where  
		(ca.crn = :crn
		or ca.noms_number = :noms_number )
	and (:start_date is null or ca.created_at >= :start_date) 
	and (:end_date is null or ca.created_at <= :end_date);

-- CAS2 Notes
select 
	ca.noms_number, 
	ca.crn, 
	can.body, 
	can.created_at,
	coalesce(nu."name",
	eu."name") as "name"
from 
	cas_2_application_notes can
inner join cas_2_applications ca
on
	ca.id = can.application_id
left join
	nomis_users nu
on 
	nu.id = can.created_by_nomis_user_id
left join 
	external_users eu 
on 
	eu.id = can.created_by_external_user_id
where 
		(ca.crn = :crn
		or ca.noms_number = :noms_number )
	and (:start_date is null or ca.created_at >= :start_date) 
	and (:end_date is null or ca.created_at <= :end_date);


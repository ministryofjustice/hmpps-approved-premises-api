-- queries for SAR - all have params specifying:
-- CRN or NOMIS NUMBER, optionally 
-- created dates between a start date and end date

	
-- approved premise applications
select
    json_agg(app) as json
from
    (
        select
            a.id,
            apa.name,
            a.crn,
            a.noms_number,
            a."data",
            a."document",
            a.created_at,
            a.submitted_at,
            created_by_user.name as created_by_user,
            app_user."name" as application_user_name ,
            apa.event_number,
            apa.is_womens_application,
            apa.offence_id,
            apa.conviction_id,
            apa.risk_ratings,
            apa.release_type,
            apa.arrival_date,
            apa.is_withdrawn,
            apa.withdrawal_reason,
            apa.other_withdrawal_reason ,
            apa.is_emergency_application,
            apa.target_location ,
            apa.status,
            apa.inmate_in_out_status_on_submission,
            apa.sentence_type,
            apa.notice_type,
            apa.ap_type,
            case_manager."name" as case_manager_name,
            apa.case_manager_is_not_applicant
        from
            approved_premises_applications apa
                join
            applications a on
                a.id = apa.id
                left join
            cas_1_application_user_details case_manager on
                case_manager.id = apa.case_manager_cas1_application_user_details_id
                left join
            cas_1_application_user_details app_user on
                app_user.id = apa.applicant_cas1_application_user_details_id
                left join
            users created_by_user on
                created_by_user.id = a.created_by_user_id
        where
            (a.crn = :crn
                or a.noms_number = :noms_number )
          and (:start_date is null
            or a.created_at >= :start_date)
          and (:end_date is null
            or a.created_at <= :end_date)
    ) app;
	
--applications timeline notes (AP only)
select
    json_agg(app) as json
from (
    select a.id as application_id,
             a.service,
             a.crn,
             a.noms_number,
             atn.body,
             atn.created_at,
             u."name" as user_name
      from application_timeline_notes atn
               inner join users u on
          u.id = atn.created_by_user_id
               inner join applications a
                          on
                              atn.application_id = a.id
      where (a.crn = :crn
          or a.noms_number = :noms_number)
        and (:start_date is null or a.created_at >= :start_date)
        and (:end_date is null or a.created_at <= :end_date)
      )
 app;
-- appeals
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
	(app.crn = :crn
		or app.noms_number = :noms_number )
	and (:start_date is null or app.created_at >= :start_date) 
	and (:end_date is null or app.created_at <= :end_date)
	
-- AP assessments
select
    json_agg(assess) as json
from (select app.crn,
             app.noms_number,
             u."name" as assessor_name,
             assess."data",
             assess."document",
             assess.created_at,
             assess.allocated_at,
             assess.submitted_at,
             assess.reallocated_at,
             assess.due_at,
             assess.decision,
             assess.rejection_rationale,
             assess.is_withdrawn,
             assess.service,
             assess.is_withdrawn,
             apa.created_from_appeal
      from assessments assess
               inner join applications app
                          on
                              app.id = assess.application_id
               inner join users u on
          u.id = assess.allocated_to_user_id
               left join approved_premises_assessments apa on
          apa.assessment_id = assess.id
      where assess.service = 'approved-premises'
        and (app.crn = :crn
          or app.noms_number = :noms_number)
        and (:start_date is null or app.created_at >= :start_date)
        and (:end_date is null or app.created_at <= :end_date)
      ) assess;
-- assessment clarification notes ap
select
    json_agg(assess) as json
    from (
        select
            app.id as application_id,
            a.id as assessment_id,
            app.crn,
            app.noms_number,
            acn.created_at,
            acn.query,
            acn.response,
            u."name" as created_by_user
        from
            assessment_clarification_notes acn
        inner join assessments a
        on
            a.id = acn.assessment_id
        inner join applications app on
            app.id = a.application_id
        inner join users u on
            u.id = acn.created_by_user_id
        where
            a.service = 'approved-premises'
        and (app.crn = :crn
            or app.noms_number = :noms_number)
        and (:start_date is null or app.created_at >= :start_date)
        and (:end_date is null or app.created_at <= :end_date)
      ) assess;
	
-- assessment referral history notes -approved premises
select
	app.id,
	assess.id as assessment_id,
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
	assess.service = 'approved-premises'
	and	
		(app.crn = :crn
		or app.noms_number = :noms_number )
	and (:start_date is null or app.created_at >= :start_date) 
	and (:end_date is null or app.created_at <= :end_date)	
	
-- temporary accomodation specific applications
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
	and (:end_date is null or a.created_at <= :end_date)
	
-- temporary accommodation assessments
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
	and (:end_date is null or app.created_at <= :end_date)
	
-- assessment referral history notes - temporary accomodatiom
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
	and (:end_date is null or app.created_at <= :end_date)
	
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
	and (:end_date is null or ca.created_at <= :end_date)

	
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
	and (:end_date is null or ca.created_at <= :end_date)


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
	and (:end_date is null or ca.created_at <= :end_date)

		
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
	and (:end_date is null or ca.created_at <= :end_date)	

	
-- placement requests
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
		(a.crn = :crn
		or a.noms_number = :noms_number )
	and (:start_date is null or a.created_at >= :start_date) 
	and (:end_date is null or a.created_at <= :end_date)
		
-- placements applications
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
	pa.withdrawal_reason
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
		(a.crn = :crn
		or a.noms_number = :noms_number )
	and (:start_date is null or a.created_at >= :start_date) 
	and (:end_date is null or a.created_at <= :end_date)
	
-- bookings - Temporary Accomodation
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
	and (:end_date is null or b.created_at <= :end_date)
	
-- bookings - Approved Premises
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
	b.service = 'approved-premises'
	and 
		(b.crn = :crn
			or b.noms_number = :noms_number )
	and (:start_date is null or b.created_at >= :start_date) 
	and (:end_date is null or b.created_at <= :end_date)
	
-- cancellations
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
		(b.crn = :crn
		or b.noms_number = :noms_number )
	and (:start_date is null or b.created_at >= :start_date) 
	and (:end_date is null or b.created_at <= :end_date)
		
-- offline applications - AP (CRN only)

select
	oa.crn,
	oa.created_at
from
	offline_applications oa
	where :crn is not null and oa.crn = :crn
	
-- offline bookings  - AP
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
inner join
offline_applications oa on
	oa.id = b.offline_application_id
left join premises p on
	b.premises_id = p.id
where 
		(b.crn = :crn
		or b.noms_number = :noms_number )
	and (:start_date is null or b.created_at >= :start_date) 
	and (:end_date is null or b.created_at <= :end_date)	

-- applications domain events
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
inner join applications a on a.id  = de.application_id 
where 
		(de.crn = :crn
		or de.noms_number = :noms_number )
	and (:start_date is null or de.created_at >= :start_date) 
	and (:end_date is null or de.created_at <= :end_date)
		
-- assessment domain events
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
inner join assessments  a 
on a.id  = de.assessment_id 
inner join applications app 
on a.application_id = app.id
where 
		(de.crn = :crn
		or de.noms_number = :noms_number )
	and (:start_date is null or de.created_at >= :start_date) 
	and (:end_date is null or de.created_at <= :end_date)
		
-- booking domain events
select  
	de.crn , 
	de.noms_number,
	de.occurred_at, 
	de."data" , 
	de."type", 
	de.created_at,
	de.service ,
	coalesce(u."name", 'Unknown') as triggered_by
from 
domain_events de
left join users u on u.id = de.triggered_by_user_id 
inner join bookings b 
on b.id  = de.booking_id  
inner join applications a    
on b.application_id = a.id
where 
		(de.crn = :crn
		or de.noms_number = :noms_number )
	and (:start_date is null or de.created_at >= :start_date) 
	and (:end_date is null or de.created_at <= :end_date)
	
-- extensions - use NOMS or CRN for booking related tables as no gauarantee on application being present.
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
	(b.crn = :crn
		or b.noms_number = :noms_number )
	and (:start_date is null or b.created_at >= :start_date) 
	and (:end_date is null or b.created_at <= :end_date)

-- departures - use domain events
select
	b.crn,
	b.noms_number,
	d.date_time ,
	d.notes,
	dr."name" as departure_reason,
	moc."name" as move_on_category,
	d.created_at
from
	departures d
inner join departure_reasons dr on
	dr.id = d.departure_reason_id
inner join move_on_categories moc on
	moc.id = d.move_on_category_id
inner join bookings b on
	b.id = d.booking_id
where 
	(b.crn = :crn
		or b.noms_number = :noms_number )
	and (:start_date is null or b.created_at >= :start_date) 
	and (:end_date is null or b.created_at <= :end_date)
	
-- arrivals - may need to use domain events
select
	b.crn,
	b.noms_number,
	arr.arrival_date_time,
	arr.expected_departure_date,
	arr.notes,
	arr.created_at
from
	arrivals arr
inner join bookings b on
	b.id = arr.booking_id
where 
	(b.crn = :crn
		or b.noms_number = :noms_number )
	and (:start_date is null or b.created_at >= :start_date) 
	and (:end_date is null or b.created_at <= :end_date)

-- non arrivals may need to use domain events
	
select 
	b.crn, 
	b.noms_number,
	na.notes, 
	na."date", 
	na.created_at, 
	nar."name" as non_arrival_reason  
from 
	non_arrivals na 
inner join 
	bookings b on b.id  = na.booking_id 
inner join 
	non_arrival_reasons nar 
on 
	nar.id = na.non_arrival_reason_id 
where 
	(b.crn = :crn
		or b.noms_number = :noms_number )
	and (:start_date is null or b.created_at >= :start_date) 
	and (:end_date is null or b.created_at <= :end_date)

-- bed moves
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
	(b.crn = :crn
		or b.noms_number = :noms_number )
	and (:start_date is null or b.created_at >= :start_date) 
	and (:end_date is null or b.created_at <= :end_date)


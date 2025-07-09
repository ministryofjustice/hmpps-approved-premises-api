package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: adhocBookingCreate,applicationWithdrawOthers,assessAppealedApplication,assessApplication,assessPlacementApplication,assessPlacementRequest,bookingCreate,bookingChangeDates,bookingWithdraw,changeRequestList,changeRequestView,offlineApplicationView,outOfServiceBedCreate,outOfServiceBedCreateBedOnHold,outOfServiceBedCancel,placementAppealCreate,placementAppealAssess,placementRequestRecordUnableToMatch,processAnAppeal,transferCreate,transferAssess,userList,userManagement,viewAssignedAssessments,viewCruDashboard,viewManageTasks,viewOutOfServiceBeds,requestForPlacementWithdrawOthers,spaceBookingCreate,spaceBookingList,spaceBookingRecordArrival,spaceBookingRecordDeparture,spaceBookingRecordNonArrival,spaceBookingRecordKeyworker,spaceBookingView,spaceBookingWithdraw,spaceBookingShorten,premisesCapacityReportView,tasksList,premisesView,premisesManage,reportsView,reportsViewWithPii
*/
enum class ApprovedPremisesUserPermission(@get:JsonValue val value: kotlin.String) {

    adhocBookingCreate("cas1_adhoc_booking_create"),
    applicationWithdrawOthers("cas1_application_withdraw_others"),
    assessAppealedApplication("cas1_assess_appealed_application"),
    assessApplication("cas1_assess_application"),
    assessPlacementApplication("cas1_assess_placement_application"),
    assessPlacementRequest("cas1_assess_placement_request"),
    bookingCreate("cas1_booking_create"),
    bookingChangeDates("cas1_booking_change_dates"),
    bookingWithdraw("cas1_booking_withdraw"),
    changeRequestList("cas1_change_request_list"),
    changeRequestView("cas1_change_request_view"),
    offlineApplicationView("cas1_offline_application_view"),
    outOfServiceBedCreate("cas1_out_of_service_bed_create"),
    outOfServiceBedCreateBedOnHold("cas1_out_of_service_bed_create_bed_on_hold"),
    outOfServiceBedCancel("cas1_out_of_service_bed_cancel"),
    placementAppealCreate("cas1_placement_appeal_create"),
    placementAppealAssess("cas1_placement_appeal_assess"),
    placementRequestRecordUnableToMatch("cas1_placement_request_record_unable_to_match"),
    processAnAppeal("cas1_process_an_appeal"),
    transferCreate("cas1_transfer_create"),
    transferAssess("cas1_transfer_assess"),
    userList("cas1_user_list"),
    userManagement("cas1_user_management"),
    viewAssignedAssessments("cas1_view_assigned_assessments"),
    viewCruDashboard("cas1_view_cru_dashboard"),
    viewManageTasks("cas1_view_manage_tasks"),
    viewOutOfServiceBeds("cas1_view_out_of_service_beds"),
    requestForPlacementWithdrawOthers("cas1_request_for_placement_withdraw_others"),
    spaceBookingCreate("cas1_space_booking_create"),
    spaceBookingList("cas1_space_booking_list"),
    spaceBookingRecordArrival("cas1_space_booking_record_arrival"),
    spaceBookingRecordDeparture("cas1_space_booking_record_departure"),
    spaceBookingRecordNonArrival("cas1_space_booking_record_non_arrival"),
    spaceBookingRecordKeyworker("cas1_space_booking_record_keyworker"),
    spaceBookingView("cas1_space_booking_view"),
    spaceBookingWithdraw("cas1_space_booking_withdraw"),
    spaceBookingShorten("cas1_space_booking_shorten"),
    premisesCapacityReportView("cas1_premises_capacity_report_view"),
    tasksList("cas1_tasks_list"),
    premisesView("cas1_premises_view"),
    premisesManage("cas1_premises_manage"),
    reportsView("cas1_reports_view"),
    reportsViewWithPii("cas1_reports_view_with_pii");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): ApprovedPremisesUserPermission {
                return values().first{it -> it.value == value}
        }
    }
}


-- Update Staffordshire and Stoke pdu to be Staffordshire North pdu
Update probation_delivery_units
Set name = 'Staffordshire North',
    delius_code = 'N52STF'
Where id = 'ce151fa8-1075-4820-8b8b-add88a3608c6';

-- Create Staffordshire South pdu
Insert Into probation_delivery_units (id, name, probation_region_id, delius_code)
Values('dcb88744-2abe-414f-829b-eb11a56ce445', 'Staffordshire South','734261a0-d053-4aed-968d-ffc518cc17f8','N52STFS');
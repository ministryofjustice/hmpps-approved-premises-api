#!/usr/bin/env bash
aws --endpoint-url=http://localhost:4566 sns publish \
    --topic-arn arn:aws:sns:eu-west-2:000000000000:inbound-topic \
    --message-attributes '{"eventType" : { "DataType":"String", "StringValue":"OFFENDER_MOVEMENT-DISCHARGE"}}' \
    --message '{"type":"OFFENDER_MOVEMENT-DISCHARGE","id":"2","contents":"discharge_message_contents"}'
# How to verify email sending in non-prod environments

We can verify emails would have been sent in two ways, depending on how the environment is configured:

## If the `NotifyMode` is `DISABLED`

We can query Application Insights with the following query:

```
traces
| where customDimensions["Logger Message"] contains "Email sending is disabled"
| order by timestamp desc
| project timestamp, operation_Name, customDimensions["Logger Message"]
```

## If the `NotifyMode` is `TEST_AND_GUEST_LIST` or `ENABLED`

We can query Application Insights with the following query:

```
exceptions
| where customDimensions["Logger Message"] contains "Unable to send email"
| order by timestamp desc
| project timestamp, operation_Name, customDimensions["Logger Message"]
```
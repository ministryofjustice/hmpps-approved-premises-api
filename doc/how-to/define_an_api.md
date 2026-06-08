# How to Define an API

this page provides a single point of reference for standards used when building APIs

## Model Naming

* Suffix all models with `Dto`
* If the model represents the root of a Request or Response, suffix with `RequestDto` or `ResponseDto`
* Never return native types directly like `List<*>`. Instead, wrap them a custom response type to support future extensibility

## Enumerations

Use the following established pattern:

```
enum class EventChangeRequestType(@get:JsonValue val value: String) {
    PLACEMENT_APPEAL("placement_appeal"),
    PLACEMENT_EXTENSION("placement_extension"),
    PLANNED_TRANSFER("planned_transfer"),
    ;
    
    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: String) = entries.first { it.value == value }
    }
}
```

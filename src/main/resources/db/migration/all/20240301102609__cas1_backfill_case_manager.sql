update approved_premises_applications apa set
    case_manager_name = cast(a.data -> 'basic-information' -> 'case-manager-information' ->> 'name' as text),
    case_manager_email = cast(a.data -> 'basic-information' -> 'case-manager-information' ->> 'emailAddress' as text),
    case_manager_telephone_number  = cast(a.data -> 'basic-information' -> 'case-manager-information' ->> 'phoneNumber' as text)
from applications a
where
    a.id = apa.id AND
    cast(a.data -> 'basic-information' -> 'case-manager-information' ->> 'name' as text) is not null AND
    cast(a.data -> 'basic-information' -> 'case-manager-information' ->> 'emailAddress' as text) is not null AND
    cast(a.data -> 'basic-information' -> 'case-manager-information' ->> 'phoneNumber' as text) is not null
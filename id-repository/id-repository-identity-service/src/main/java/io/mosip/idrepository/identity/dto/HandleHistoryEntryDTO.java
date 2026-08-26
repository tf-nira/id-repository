package io.mosip.idrepository.identity.dto;

import io.mosip.idrepository.core.dto.ResponseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class HandleHistoryEntryDTO extends ResponseDTO {
    /** eff_dtimes of this uin_h snapshot, ISO-8601 string */
    private String effectiveDateTime;
}
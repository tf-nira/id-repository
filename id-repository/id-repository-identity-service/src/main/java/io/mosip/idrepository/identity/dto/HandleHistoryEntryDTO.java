package io.mosip.idrepository.identity.dto;

import java.time.LocalDateTime;
import java.util.List;

import io.mosip.idrepository.core.dto.CardDetailDto;
import lombok.Data;

@Data
public class HandleHistoryEntryDTO {
    private String regId;
    private LocalDateTime effectiveDateTime;
    private String status;
    private Object identity;
    private List<String> verifiedAttributes;
    private List<CardDetailDto> cardDetails;
}
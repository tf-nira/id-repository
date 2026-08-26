package io.mosip.idrepository.identity.dto;

import java.util.List;
import lombok.Data;

@Data
public class IdResponseHistoryDTO {
    private String id;
    private String version;
    private List<HandleHistoryEntryDTO> response;
}
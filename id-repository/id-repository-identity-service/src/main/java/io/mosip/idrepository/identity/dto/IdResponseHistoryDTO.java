package io.mosip.idrepository.identity.dto;

import java.time.LocalDateTime;
import java.util.List;

import io.mosip.idrepository.core.dto.DocumentsDTO;
import lombok.Data;

@Data
public class IdResponseHistoryDTO {
    private String id;
    private String version;
    private LocalDateTime responsetime;
    private List<HandleHistoryEntryDTO> response;
    private List<DocumentsDTO> documents;
}
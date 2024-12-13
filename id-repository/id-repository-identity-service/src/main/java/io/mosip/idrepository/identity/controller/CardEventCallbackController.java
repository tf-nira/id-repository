package io.mosip.idrepository.identity.controller;

import static io.mosip.idrepository.core.constant.IdRepoConstants.CARD_EVENT_SECRET;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CARD_EVENT_TOPIC;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.spi.IdRepoService;
import io.mosip.kernel.core.websub.model.EventModel;
import io.mosip.kernel.websub.api.annotation.PreAuthenticateContentAndVerifyIntent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "id-repo-card-event-callback-controller", description = "ID Repo Card Event Callback Controller")
public class CardEventCallbackController {

	/** The id repo service. */
	@Autowired
	private IdRepoService<IdRequestDTO, IdResponseDTO> idRepoServiceImpl;

	@SuppressWarnings("unchecked")
	@PostMapping(path = "/callback/card_number_update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "handleCardNumberEvent", description = "handleCardNumberEvent", tags = {
			"id-repo-controller" })
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request authenticated successfully"),
			@ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(hidden = true))) })
	@PreAuthenticateContentAndVerifyIntent(secret = "${" + CARD_EVENT_SECRET
			+ "}", callback = "/idrepository/v1/identity/callback/card_number_update", topic = "${" + CARD_EVENT_TOPIC
					+ "}")
	public void handleCardNumberEvent(@RequestBody EventModel eventModel) {
		idRepoServiceImpl.updateCardNumber(eventModel.getEvent().getData());
	}
}

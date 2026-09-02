package io.mosip.idrepository.identity.controller;

import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_EVENT_SECRET;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_EVENT_TOPIC;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.spi.OnDemandCredentialService;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.websub.model.EventModel;
import io.mosip.kernel.websub.api.annotation.PreAuthenticateContentAndVerifyIntent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "id-repo-ondemand-credential-callback-controller", description = "ID Repo Ondemand Credential Callback Controller")
public class OnDemandCredentialCallbackController {

    Logger mosipLogger = IdRepoLogger.getLogger(OnDemandCredentialCallbackController.class);

    @Autowired
    OnDemandCredentialService onDemandCredentialService;

    @SuppressWarnings("unchecked")
    @PostMapping(path = "/callback/issue_credential", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "issueCredential", description = "issueCredential", tags = {
            "id-repo-ondemand-credential-callback-controller" })
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request authenticated successfully"),
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(hidden = true))) })
    @PreAuthenticateContentAndVerifyIntent(secret = "${" + CREDENTIAL_EVENT_SECRET
            + "}", callback = "/idrepository/v1/identity/callback/issue_credential", topic = "${" + CREDENTIAL_EVENT_TOPIC
            + "}")
    public void issueCredential(@RequestBody EventModel eventModel) {
        mosipLogger.info("Even received: " + eventModel.getEvent().getId());
        onDemandCredentialService.issueCredential(eventModel.getEvent().getData());
    }
}
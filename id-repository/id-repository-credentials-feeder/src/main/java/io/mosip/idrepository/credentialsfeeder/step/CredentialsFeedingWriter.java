package io.mosip.idrepository.credentialsfeeder.step;

import static io.mosip.idrepository.core.constant.IdRepoConstants.SPLITTER;
import static io.mosip.idrepository.core.constant.IdRepoConstants.UIN_REFID;
import static io.mosip.idrepository.credentialsfeeder.constant.Constants.MOSIP_IDREPO_IDENTITY_UIN_STATUS_REGISTERED;
import static io.mosip.idrepository.credentialsfeeder.constant.Constants.MOSIP_IDREPO_VID_ACTIVE_STATUS;
import static io.mosip.idrepository.credentialsfeeder.constant.Constants.PROP_ONLINE_VERIFICATION_PARTNER_IDS;
import static io.mosip.idrepository.credentialsfeeder.constant.Constants.UNLOCK_EXP_TIMESTAMP;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.mosip.idrepository.core.dto.*;
import io.mosip.idrepository.core.entity.Handle;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.IDAEventType;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.manager.CredentialServiceManager;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.idrepository.core.manager.CredentialStatusManager;
import io.mosip.idrepository.core.repository.HandleRepo;
import io.mosip.idrepository.core.repository.UinEncryptSaltRepo;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.credentialsfeeder.entity.AuthtypeLock;
import io.mosip.idrepository.credentialsfeeder.entity.CredentialFeederProgress;
import io.mosip.idrepository.credentialsfeeder.entity.Uin;
import io.mosip.idrepository.credentialsfeeder.repository.AuthLockRepository;
import io.mosip.idrepository.credentialsfeeder.repository.CredentialFeederProgressRepo;

/**
 * The Class CredentialsFeedingWriter - Class to feed credentials using
 * credential requests. Implements {@code ItemWriter}.
 *
 * @author Loganathan Sekar
 * @author Manoj SP
 */
@Component
public class CredentialsFeedingWriter implements ItemWriter<Uin> {

	private static final String CREDENTIALS_FEEDER = "CREDENTIALS_FEEDER";

	private static final Logger mosipLogger = IdRepoLogger.getLogger(CredentialsFeedingWriter.class);

	@Value("${" + PROP_ONLINE_VERIFICATION_PARTNER_IDS + "}")
	private String[] onlineVerificationPartnerIds;

	@Value("${" + MOSIP_IDREPO_IDENTITY_UIN_STATUS_REGISTERED + "}")
	private String uinActiveStatus;

	@Value("${" + MOSIP_IDREPO_VID_ACTIVE_STATUS + "}")
	private String vidActiveStatus;

	/** The uin hash salt repo. */
	@Autowired
	private UinHashSaltRepo uinHashSaltRepo;

	@Autowired
	private CredentialServiceManager credentialServiceManager;

	@Autowired
	private CredentialStatusManager credentialStatusManager;

	@Autowired
	private RestRequestBuilder restBuilder;

	@Autowired
	private RestHelper restHelper;
	
	@Autowired
	private IdRepoWebSubHelper webSubHelper;
	
	@Autowired
	private IdRepoSecurityManager securityManager;
	
	@Autowired
	private AuthLockRepository authLockRepo;
	
	@Autowired(required = false)
	private HandleRepo handleRepo;
	
	@Autowired
	private UinEncryptSaltRepo uinEncryptSaltRepo;
	
	@Autowired
	private CredentialFeederProgressRepo progressRepo;
	
	@Value("${idrepo.credential.feeder.instance-id4}")
	private String instanceId; 
	
	@Value("${" + UIN_REFID + "}")
	private String uinRefId;

	private static final String SEND_REQUEST_TO_CRED_SERVICE = "sendRequestToCredService";
	
	/**
	 * For each Uin in the list, decrypt it, and then issue a credential for it
	 * 
	 * @param requestIdEntities The list of Uin objects that are to be processed.
	 */
	@Override
	public void write(List<? extends Uin> requestIdEntities) throws Exception {
		mosipLogger.info(CREDENTIALS_FEEDER, "CredentialsFeedingWriter", "WRITE START",
				"Starting write for chunk | item count: " + requestIdEntities.size());
		//requestIdEntities.stream().map(this::decryptUin).forEach(this::issueCredential);
		
		requestIdEntities.stream()
        .forEach(uinEntity -> {
            String uin = decryptUin(uinEntity);
            String regId = uinEntity.getRegId() + "-PDF";
            issueCredential(uin, regId);
        });
		
		if (!requestIdEntities.isEmpty()) {
			LocalDateTime maxProcessedDate = requestIdEntities.stream()
					.map(Uin::getCreatedDateTime)
					.filter(Objects::nonNull)
					.max(LocalDateTime::compareTo)
					.orElse(null);
			
			if (maxProcessedDate != null) {
				CredentialFeederProgress progress = progressRepo.findById(instanceId).orElseThrow();
				progress.setFromDate(maxProcessedDate);
				progress.setProcessedCount(progress.getProcessedCount() == null ? 0
						: progress.getProcessedCount() + requestIdEntities.size());
				progress.setUpdateDateTime(LocalDateTime.now());
				progressRepo.save(progress);
			}
		}
		
		mosipLogger.info(CREDENTIALS_FEEDER, "CredentialsFeedingWriter", "WRITE END",
				"Write completed for chunk | item count: " + requestIdEntities.size());
	}

	/**
	 * The function issues a UIN and VID credential to the user
	 * 
	 * @param uin The Aadhaar number of the resident.
	 */
	private void issueCredential(String uin, String regId) {
		mosipLogger.info(CREDENTIALS_FEEDER, "CredentialsFeedingWriter", "ISSUE CREDENTIAL START",
				"Issuing UIN credential, VID credential and publishing auth-lock for UIN: "
						+ uin);
		issueUinCredential(uin, regId);
		//issueVidCredential(uin);
		publishAuthLock(uin);
		mosipLogger.info(CREDENTIALS_FEEDER, "CredentialsFeedingWriter", "ISSUE CREDENTIAL END",
				"Credential issuance and auth-lock publish completed for UIN: " + uin);
	}

	/**
	 * This function is responsible for issuing credential to the partner
	 * 
	 * @param uin The UIN of the resident
	 */
	private void issueUinCredential(String uin, String regId) {
		mosipLogger.info(CREDENTIALS_FEEDER, "CredentialsFeedingWriter", "ISSUE UIN CREDENTIAL",
				"Sending UIN events to credential service"
						+ " | partners: " + Arrays.toString(onlineVerificationPartnerIds)
						+ " | UIN:  " + uin);
		AtomicInteger credentialCount = new AtomicInteger(0);
		BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> loggingConsumer = (request, response) -> {
			int count = credentialCount.incrementAndGet();
			mosipLogger.info(CREDENTIALS_FEEDER, "CredentialsFeedingWriter", "CREDENTIAL ISSUED",
					"Credential request #" + count + " issued"
							+ " | partner: " + (request.getRequest() != null ? request.getRequest().getIssuer() : "N/A")
							+ " | requestId: " + (request.getRequest() != null ? request.getRequest().getRequestId() : "N/A")
							+ " | response: " + response);
			credentialStatusManager.credentialRequestResponseConsumer(request, response);
		};
		mosipLogger.info("Retrived registration id : {}", regId);
		credentialServiceManager.sendUinEventsToCredService(uin, null, false, null, getHandles(uin, uinHashSaltRepo::retrieveSaltById),
				Arrays.asList(onlineVerificationPartnerIds), uinHashSaltRepo::retrieveSaltById,
				loggingConsumer,
	            regId);
		if (credentialCount.get() == 0) {
			mosipLogger.warn(CREDENTIALS_FEEDER, "CredentialsFeedingWriter", "ISSUE UIN CREDENTIAL WARNING",
					"No UIN credentials were issued — disableUINBasedCredentialRequest may be true or vidInfoDtos/handleList are null. UIN: " + uin);
		} else {
			mosipLogger.info(CREDENTIALS_FEEDER, "CredentialsFeedingWriter", "ISSUE UIN CREDENTIAL DONE",
					credentialCount.get() + " UIN credential(s) issued successfully for UIN: " + uin);
		}
	}

	/**
	 * It issues a VID credential.
	 * 
	 * @param uin The UIN of the resident
	 */
	private void issueVidCredential(String uin) {
		mosipLogger.info(CREDENTIALS_FEEDER, "CredentialsFeedingWriter", "ISSUE VID CREDENTIAL",
				"Fetching VIDs and sending VID events to credential service"
						+ " | partners: " + Arrays.toString(onlineVerificationPartnerIds)
						+ " | UIN: " + uin);
		try {
			RestRequestDTO restRequest = restBuilder.buildRequest(RestServicesConstants.RETRIEVE_VIDS_BY_UIN, null,
					VidsInfosDTO.class);
			restRequest.setUri(restRequest.getUri().replace("{uin}", uin));
			VidsInfosDTO response = restHelper.requestSync(restRequest);
			List<VidInfoDTO> vidInfoDtos = response.getResponse();
			mosipLogger.info(CREDENTIALS_FEEDER, "CredentialsFeedingWriter", "ISSUE VID CREDENTIAL",
					"Retrieved VIDs for UIN: " + uin
							+ " | VID count: " + (vidInfoDtos == null ? 0 : vidInfoDtos.size()));
			credentialServiceManager.sendVidEventsToCredService(uin, vidActiveStatus, vidInfoDtos, false,
					Arrays.asList(onlineVerificationPartnerIds), uinHashSaltRepo::retrieveSaltById,
					credentialStatusManager::credentialRequestResponseConsumer);
			mosipLogger.info(CREDENTIALS_FEEDER, "CredentialsFeedingWriter", "ISSUE VID CREDENTIAL DONE",
					"VID events sent to credential service successfully for UIN: " + uin);
		} catch (RestServiceException | IdRepoDataValidationException e) {
			mosipLogger.error(CREDENTIALS_FEEDER, "CredentialsFeedingWriter", "ISSUE VID CREDENTIAL ERROR",
					"Failed to issue VID credential for UIN: " + uin
							+ " | error: " + ExceptionUtils.getStackTrace(e));
			throw new IdRepoAppUncheckedException(IdRepoErrorConstants.UNKNOWN_ERROR, e);
		}
	}
	
	/**
	 * This function finds the auth lock status details from DB based on UIN and
	 * publishes to web sub.
	 * 
	 * @param uin The UIN of the resident
	 */
	private void publishAuthLock(String uin) {
		mosipLogger.info(CREDENTIALS_FEEDER, "CredentialsFeedingWriter", "PUBLISH AUTH LOCK",
				"Publishing auth-lock status to WebSub for UIN: " + uin);
		String uinHash = securityManager.hash(uin.getBytes());
		List<AuthtypeLock> records = authLockRepo.findByHashedUin(uinHash);
		mosipLogger.info(CREDENTIALS_FEEDER, "CredentialsFeedingWriter", "PUBLISH AUTH LOCK",
				"Auth-lock records found: " + records.size() + " for UIN: " + uin);
		List<AuthtypeStatus> authTypeStatusList = records.stream()
				.map(authLock -> new AuthtypeStatus(authLock.getAuthtypecode(),
						Boolean.valueOf(authLock.getStatuscode()),
						Objects.isNull(authLock.getUnlockExpiryDTtimes()) ? null
								: Map.of(UNLOCK_EXP_TIMESTAMP, authLock.getUnlockExpiryDTtimes())))
				.collect(Collectors.toList());
		Stream.of(onlineVerificationPartnerIds).filter(partnerId -> !authTypeStatusList.isEmpty())
				.forEach(partnerId -> {
					String topic = partnerId + "/" + IDAEventType.AUTH_TYPE_STATUS_UPDATE.name();
					mosipLogger.info(CREDENTIALS_FEEDER, "CredentialsFeedingWriter", "PUBLISH AUTH LOCK",
							"Publishing auth-type status update event to topic: " + topic
									+ " | status count: " + authTypeStatusList.size());
					webSubHelper.publishAuthTypeStatusUpdateEvent(uinHash, authTypeStatusList, topic, partnerId);
				});
		mosipLogger.info(CREDENTIALS_FEEDER, "CredentialsFeedingWriter", "PUBLISH AUTH LOCK DONE",
				"Auth-lock status publish completed for UIN: " + uin);
	}

	/**
	 * It decrypts the UIN and returns the
	 * decrypted UIN
	 * 
	 * @param entity The entity that you want to decrypt.
	 * @return The decrypted UIN
	 */
	private String decryptUin(Uin entity) {
		mosipLogger.info(CREDENTIALS_FEEDER, "CredentialsFeedingWriter", "DECRYPT UIN",
				"Decrypting UIN entity with createdDateTime: " + entity.getCreatedDateTime());
		try {
			String decryptedUin = credentialStatusManager.decryptId(entity.getUin());
			mosipLogger.info(CREDENTIALS_FEEDER, "CredentialsFeedingWriter", "DECRYPT UIN DONE",
					"UIN decryption successful for entity with createdDateTime: " + entity.getCreatedDateTime());
			return decryptedUin;
		} catch (IdRepoAppException e) {
			mosipLogger.error(CREDENTIALS_FEEDER, "CredentialsFeedingWriter", "DECRYPT UIN ERROR",
					"Failed to decrypt UIN for entity with createdDateTime: " + entity.getCreatedDateTime()
							+ " | error: " + ExceptionUtils.getStackTrace(e));
			throw new IdRepoAppUncheckedException(e.getErrorCode(), e.getErrorText(), e);
		}
	}
	
	private List<HandleInfoDTO> getHandles(String uin, IntFunction<String> saltRetreivalFunction) {
		if(handleRepo == null) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), "getHandles",
					"HandleRepo is NULL");
			return List.of();
		}

		int modResult = securityManager.getSaltKeyForId(uin);
		String hashSalt = uinHashSaltRepo.retrieveSaltById(modResult);
		String uinHash = modResult + SPLITTER + securityManager.hashwithSalt(uin.getBytes(), hashSalt.getBytes());
		List<Handle> list = handleRepo.findByUinHash(uinHash);

		List<HandleInfoDTO> handleInfoDTOS = new ArrayList<>();
		for(Handle entity : list) {
			HandleInfoDTO handleInfoDTO = new HandleInfoDTO();
			String encryptSalt = uinEncryptSaltRepo
					.retrieveSaltById(Integer.valueOf(io.mosip.kernel.core.util.StringUtils.substringBefore(entity.getHandle(), SPLITTER)));
			try {
				handleInfoDTO.setHandle(new String(securityManager.decryptWithSalt(
						CryptoUtil.decodeURLSafeBase64(io.mosip.kernel.core.util.StringUtils.substringAfter(entity.getHandle(), SPLITTER)),
						CryptoUtil.decodePlainBase64(encryptSalt), uinRefId)));

				handleInfoDTO.setAdditionalData(securityManager.getIdHashAndAttributesWithSaltModuloByPlainIdHash(handleInfoDTO.getHandle(),
						saltRetreivalFunction));
				handleInfoDTO.getAdditionalData().put("idType", IdType.HANDLE.getIdType());
				handleInfoDTOS.add(handleInfoDTO);
			} catch (IdRepoAppException e) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), SEND_REQUEST_TO_CRED_SERVICE, "getHandles",
						"\n Failed to decrypt handle due to " + e.getMessage());
			}
		}
		return handleInfoDTOS;
	}
}

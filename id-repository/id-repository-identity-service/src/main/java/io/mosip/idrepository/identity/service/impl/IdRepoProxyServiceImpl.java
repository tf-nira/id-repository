package io.mosip.idrepository.identity.service.impl;

import static io.mosip.idrepository.core.constant.IdRepoConstants.ACTIVE_STATUS;
import static io.mosip.idrepository.core.constant.IdRepoConstants.ADD_IDENTITY;
import static io.mosip.idrepository.core.constant.IdRepoConstants.ALL;
import static io.mosip.idrepository.core.constant.IdRepoConstants.BIO;
import static io.mosip.idrepository.core.constant.IdRepoConstants.BIOMETRICS;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREATE;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DEMO;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DEMOGRAPHICS;
import static io.mosip.idrepository.core.constant.IdRepoConstants.GET_FILES;
import static io.mosip.idrepository.core.constant.IdRepoConstants.ID_HASH;
import static io.mosip.idrepository.core.constant.IdRepoConstants.ID_REPO;
import static io.mosip.idrepository.core.constant.IdRepoConstants.ID_REPO_SERVICE_IMPL;
import static io.mosip.idrepository.core.constant.IdRepoConstants.MOSIP_ID_UPDATE;
import static io.mosip.idrepository.core.constant.IdRepoConstants.READ;
import static io.mosip.idrepository.core.constant.IdRepoConstants.RETRIEVE_IDENTITY;
import static io.mosip.idrepository.core.constant.IdRepoConstants.SPLITTER;
import static io.mosip.idrepository.core.constant.IdRepoConstants.SUPPORTED_MODALITIES;
import static io.mosip.idrepository.core.constant.IdRepoConstants.UPDATE_IDENTITY;
import static io.mosip.idrepository.core.constant.IdRepoConstants.WEB_SUB_PUBLISH_URL;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.BIO_EXTRACTION_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.DATABASE_ACCESS_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.DOCUMENT_HASH_MISMATCH;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.ID_OBJECT_PROCESSING_FAILED;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.NO_RECORD_FOUND;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.PARSE_EXCEPTION;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.RECORD_EXISTS;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import io.mosip.idrepository.identity.dto.HandleHistoryEntryDTO;
import io.mosip.idrepository.identity.dto.IdResponseHistoryDTO;
import io.mosip.idrepository.identity.entity.*;
import io.mosip.idrepository.identity.repository.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.JDBCConnectionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.EventType;
import io.mosip.idrepository.core.constant.IDAEventType;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.dto.CardDetailDto;
import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.ResponseDTO;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.entity.Handle;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.repository.HandleRepo;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.spi.BiometricExtractionService;
import io.mosip.idrepository.core.spi.IdRepoService;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.helper.IdRepoServiceHelper;
import io.mosip.idrepository.identity.helper.ObjectStoreHelper;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.retry.WithRetry;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.websub.model.Event;
import io.mosip.kernel.core.websub.model.EventModel;
import io.mosip.kernel.core.websub.model.Type;
import io.mosip.kernel.core.websub.spi.PublisherClient;

/**
 * The Class IdRepoServiceImpl - Service implementation for Identity service.
 *
 * @author Manoj SP
 */
@Service
public class IdRepoProxyServiceImpl implements IdRepoService<IdRequestDTO, IdResponseDTO> {

	public static final Logger mosipLogger = IdRepoLogger.getLogger(IdRepoProxyServiceImpl.class);

	@Autowired
	private ObjectStoreHelper objectStoreHelper;

	/** The mapper. */
	@Autowired
	private ObjectMapper mapper;

	/** The id. */
	@Resource
	private Map<String, String> id;

	/** The allowed bio types. */
	@Resource
	private List<String> allowedBioAttributes;

	/** The uin repo. */
	@Autowired
	private UinRepo uinRepo;

	@Autowired
	private UinDraftRepo uinDraftRepo;

	/** The uin history repo. */
	@Autowired
	private UinHistoryRepo uinHistoryRepo;

	/** The service. */
	@Autowired
	private IdRepoService<IdRequestDTO, Uin> service;

	/** The security manager. */
	@Autowired
	private IdRepoSecurityManager securityManager;

	/** The uin hash salt repo. */
	@Autowired
	private UinHashSaltRepo uinHashSaltRepo;

	@Autowired
	private RestHelper restHelper;

	@Autowired
	private RestRequestBuilder restBuilder;

	/** The cbeff util. */
	@Autowired
	private CbeffUtil cbeffUtil;

	@Autowired
	private BiometricExtractionService biometricExtractionService;

	@Autowired
	private PublisherClient<String, EventModel, HttpHeaders> pb;

	@Autowired
	private Environment env;

	@Autowired
	private HandleRepo handleRepo;

	@Autowired
	private CardDetailRepository cardDetailRepository;

	@Autowired
	private IdRepoServiceHelper idRepoServiceHelper;

	@Autowired
	private UinDocumentHistoryRepo uinDocHRepo;

	@Autowired
	private UinBiometricHistoryRepo uinBioHRepo;

	private static final String REGISTRATION_ID = "registration_id";

	private static final String PARTNER_ACTIVE_STATUS = "Active";

	private static final String ACTIVE = "ACTIVE";

	private static final String ACTIVATED = "ACTIVATED";

	@Value("${id-repo-ida-event-type-namespace:mosip}")
	private String idaEventTypeNamespace;

	@Value("${id-repo-ida-event-type-name:ida}")
	private String idaEventTypeName;

	@Value("${mosip.idrepo.dob.format}")
	private String dobFormat;

	private static final String INUGANDA = "In Uganda";

	private static final String OUTSIDEUGANDA = "Outside Uganda";


	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * io.mosip.kernel.core.idrepo.spi.IdRepoService#addIdentity(java.lang.Object)
	 */
	@Override
	public IdResponseDTO addIdentity(IdRequestDTO request, String uin) throws IdRepoAppException {
		try {
			String uinHash = retrieveUinHash(uin);
			if (uinRepo.existsByUinHash(uinHash)
					|| uinDraftRepo.existsByRegId(request.getRequest().getRegistrationId())
					|| uinHistoryRepo.existsByRegId(request.getRequest().getRegistrationId())) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, ADD_IDENTITY,
						RECORD_EXISTS.getErrorMessage());
				throw new IdRepoAppException(RECORD_EXISTS);
			}

			Uin uinEntity = service.addIdentity(request, uin);

			notify(uin, false, request.getRequest().getRegistrationId());
			return constructIdResponse(this.id.get(CREATE), uinEntity, null);

		} catch (IdRepoAppException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, ADD_IDENTITY, e.getErrorText());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, ADD_IDENTITY, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} catch (IdRepoAppUncheckedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, ADD_IDENTITY,
					"\n" + e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * io.mosip.kernel.core.idrepo.spi.IdRepoService#retrieveIdentity(java.lang.
	 * String)
	 */
	@Override
	public IdResponseDTO retrieveIdentity(String id, IdType idType, String type, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		switch (idType) {
			case HANDLE:
				return retrieveIdentityByHandle(id, type, extractionFormats);
			case VID:
				return retrieveIdentityByVid(id, type, extractionFormats);
			case ID:
				return retrieveIdentityByRid(id, type, extractionFormats);
			case UIN:
			default:
				return retrieveIdentityByUin(id, type, extractionFormats);
		}
	}

	/**
	 * Retrieve identity by uin.
	 *
	 * @param uin               the uin
	 * @param type              the type
	 * @param extractionFormats
	 * @return the id response DTO
	 * @throws IdRepoAppException the id repo app exception
	 */
	private IdResponseDTO retrieveIdentityByUin(String uin, String type, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		try {
			String uinHash = retrieveUinHash(uin);
			return retrieveIdentityByUinHash(type, uinHash, extractionFormats);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY,
					"\n" + e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} catch (IdRepoAppException | IdRepoAppUncheckedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY,
					"\n" + e.getMessage());
			String errorCode = (e instanceof IdRepoAppException) ? ((IdRepoAppException) e).getErrorCode()
					: ((IdRepoAppUncheckedException) e).getErrorCode();
			String errorMsg = (e instanceof IdRepoAppException) ? ((IdRepoAppException) e).getErrorText()
					: ((IdRepoAppUncheckedException) e).getErrorText();
			throw new IdRepoAppException(errorCode, errorMsg, e);
		}
	}

	/**
	 * Retrieve identity by vid.
	 *
	 * @param vid               the vid
	 * @param type              the type
	 * @param extractionFormats
	 * @return the id response DTO
	 * @throws IdRepoAppException the id repo app exception
	 */
	private IdResponseDTO retrieveIdentityByVid(String vid, String type, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		String uin = getUinByVid(vid);
		return retrieveIdentityByUin(uin, type, extractionFormats);
	}

	/**
	 * Retrieve uin hash.
	 *
	 * @param uin the uin
	 * @return the string
	 */
	private String retrieveUinHash(String uin) {
		int saltId = securityManager.getSaltKeyForId(uin);
		String hashSalt = uinHashSaltRepo.retrieveSaltById(saltId);
		String hashwithSalt = securityManager.hashwithSalt(uin.getBytes(), hashSalt.getBytes());
		return saltId + SPLITTER + hashwithSalt;
	}

	/**
	 * Retrieve identity by uin hash.
	 *
	 * @param type    the type
	 * @param uinHash the uin hash
	 * @return the id response DTO
	 * @throws IdRepoAppException the id repo app exception
	 */
	private IdResponseDTO retrieveIdentityByUinHash(String type, String uinHash, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		List<DocumentsDTO> documents = new ArrayList<>();
		Uin uinObject = service.retrieveIdentity(uinHash, IdType.UIN, type, null);
		if (StringUtils.containsIgnoreCase(type, BIO) || StringUtils.containsIgnoreCase(type, ALL)) {
			getFiles(uinObject, documents, extractionFormats, BIOMETRICS);
		}
		if (StringUtils.containsIgnoreCase(type, DEMO) || StringUtils.containsIgnoreCase(type, ALL)) {
			getFiles(uinObject, documents, null, DEMOGRAPHICS);
		}
		return constructIdResponse(this.id.get(READ), uinObject, documents);
	}

	/**
	 * Retrieve identity by rid.
	 *
	 * @param rid               the rid
	 * @param type              the type
	 * @param extractionFormats
	 * @return the id response DTO
	 * @throws IdRepoAppException the id repo app exception
	 */
	IdResponseDTO retrieveIdentityByRid(String rid, String type, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		try {
			String uinHash = uinRepo.getUinHashByRid(rid);
			if (Objects.isNull(uinHash)) {
				uinHash = uinHistoryRepo.getUinHashByRid(rid);
			}
			if (Objects.nonNull(uinHash)) {
				return retrieveIdentityByUinHash(type, uinHash, extractionFormats);
			} else {
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY,
					"\n" + e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} catch (IdRepoAppException | IdRepoAppUncheckedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY,
					"\n" + e.getMessage());
			String errorCode = (e instanceof IdRepoAppException) ? ((IdRepoAppException) e).getErrorCode()
					: ((IdRepoAppUncheckedException) e).getErrorCode();
			String errorMsg = (e instanceof IdRepoAppException) ? ((IdRepoAppException) e).getErrorText()
					: ((IdRepoAppUncheckedException) e).getErrorText();
			throw new IdRepoAppException(errorCode, errorMsg, e);
		}
	}

	/**
	 * Gets the files.
	 *
	 * @param uinObject the uin object
	 * @param documents the documents
	 * @param type      the type
	 * @return the files
	 */
	private void getFiles(Uin uinObject, List<DocumentsDTO> documents, Map<String, String> extractionFormats,
						  String type) {
		if (type.equals(BIOMETRICS)) {
			getBiometricFiles(uinObject, documents, extractionFormats);
		}

		if (type.equals(DEMOGRAPHICS)) {
			getDemographicFiles(uinObject, documents);
		}
	}

	/**
	 * Gets the demographic files.
	 *
	 * @param uinObject the uin object
	 * @param documents the documents
	 * @return the demographic files
	 */
	private void getDemographicFiles(Uin uinObject, List<DocumentsDTO> documents) {
		uinObject.getDocuments().stream().forEach(demo -> {
			try {
				String uinHash = uinObject.getUinHash().split("_")[1];
				byte[] data = objectStoreHelper.getDemographicObject(uinHash, demo.getDocId());
				if (demo.getDocHash().equals(securityManager.hash(data))) {
					documents.add(new DocumentsDTO(demo.getDoccatCode(), CryptoUtil.encodeToURLSafeBase64(data)));
				} else {
					mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES,
							DOCUMENT_HASH_MISMATCH.getErrorMessage());
					throw new IdRepoAppException(DOCUMENT_HASH_MISMATCH);
				}
			} catch (IdRepoAppException e) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES,
						"\n" + e.getMessage());
				throw new IdRepoAppUncheckedException(e.getErrorCode(), e.getErrorText(), e);
			}
		});
	}

	/**
	 * Gets the biometric files.
	 *
	 * @param uinObject         the uin object
	 * @param documents         the documents
	 * @param extractionFormats
	 * @return the biometric files
	 */
	private void getBiometricFiles(Uin uinObject, List<DocumentsDTO> documents, Map<String, String> extractionFormats) {
		uinObject.getBiometrics().stream().forEach(bio -> {
			if (allowedBioAttributes.contains(bio.getBiometricFileType())) {
				try {
					String uinHash = uinObject.getUinHash().split("_")[1];
					byte[] data = objectStoreHelper.getBiometricObject(uinHash, bio.getBioFileId());
					if (Objects.nonNull(data)) {
						if (Objects.nonNull(extractionFormats) && !extractionFormats.isEmpty()) {
							byte[] extractedData = getBiometricsForRequestedFormats(uinHash, bio.getBioFileId(),
									extractionFormats, data);
							if (Objects.nonNull(extractedData)) {
								documents.add(new DocumentsDTO(bio.getBiometricFileType(),
										CryptoUtil.encodeToURLSafeBase64(extractedData)));
							}

						} else {
							if (StringUtils.equals(bio.getBiometricFileHash(), securityManager.hash(data))) {
								documents.add(
										new DocumentsDTO(bio.getBiometricFileType(),
												CryptoUtil.encodeToURLSafeBase64(data)));
							} else {
								mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES,
										DOCUMENT_HASH_MISMATCH.getErrorMessage());
								throw new IdRepoAppException(DOCUMENT_HASH_MISMATCH);
							}
						}
					}
				} catch (IdRepoAppException e) {
					mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES, e.getMessage());
					throw new IdRepoAppUncheckedException(e.getErrorCode(), e.getErrorText(), e);
				}
			}
		});
	}

	protected byte[] getBiometricsForRequestedFormats(String uinHash, String fileName,
													  Map<String, String> extractionFormats, byte[] originalData) throws IdRepoAppException {
		try {
			List<BIR> originalBirs = cbeffUtil.getBIRDataFromXML(originalData);
			List<BIR> finalBirs = new ArrayList<>();

			List<CompletableFuture<List<BIR>>> extractionFutures = new ArrayList<>();

			for (BiometricType modality : SUPPORTED_MODALITIES) {
				List<BIR> birTypesForModality = originalBirs.stream()
						.filter(bir -> bir.getBdbInfo().getType().get(0).value().equalsIgnoreCase(modality.value()))
						.collect(Collectors.toList());
				List<BIR> filtertedBirTypesForModality = filterExceptionBiometrics(birTypesForModality, finalBirs);
				Optional<Entry<String, String>> extractionFormatForModality = extractionFormats.entrySet().stream()
						.filter(ent -> ent.getKey().toLowerCase().contains(modality.value().toLowerCase())).findAny();
				if (!filtertedBirTypesForModality.isEmpty()) {
					if (!extractionFormatForModality.isEmpty()) {
					Entry<String, String> format = extractionFormatForModality.get();
					CompletableFuture<List<BIR>> extractTemplateFuture = biometricExtractionService.extractTemplate(
							uinHash, fileName, format.getKey(), format.getValue(), filtertedBirTypesForModality);
					extractionFutures.add(extractTemplateFuture);

				} else {
					mosipLogger.info(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "extractTemplate",
							"GETTING NON EXTRACTED FORMAT for Modality: " + modality.name());
					finalBirs.addAll(filtertedBirTypesForModality);
				}
				}
			}

			CompletableFuture.allOf(extractionFutures.toArray(new CompletableFuture<?>[extractionFutures.size()]))
					.join();
			for (CompletableFuture<List<BIR>> future : extractionFutures) {
				finalBirs.addAll(future.get());
			}

			return cbeffUtil.createXML(finalBirs);
		} catch (IdRepoAppUncheckedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "extractTemplate", e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} catch (InterruptedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "extractTemplate", e.getMessage());
			throw new IdRepoAppException(BIO_EXTRACTION_ERROR, e);
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "extractTemplate", e.getMessage());
			throw new IdRepoAppException(BIO_EXTRACTION_ERROR, e);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see io.mosip.kernel.core.idrepo.spi.IdRepoService#updateIdentity(java.lang.
	 * Object, java.lang.String)
	 */
	@Override
	public IdResponseDTO updateIdentity(IdRequestDTO request, String uin) throws IdRepoAppException {
		String regId = request.getRequest().getRegistrationId();
		try {
			String uinHash = retrieveUinHash(uin);
			if (uinRepo.existsByUinHash(uinHash)) {
				if (uinRepo.existsByRegId(regId)
						|| uinDraftRepo.existsByRegId(request.getRequest().getRegistrationId())
						|| uinHistoryRepo.existsByRegId(request.getRequest().getRegistrationId())) {
					mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES,
							RECORD_EXISTS.getErrorMessage());
					throw new IdRepoAppException(RECORD_EXISTS);
				}
				Uin uinObject=service.updateIdentity(request, uin);
				mosipLogger.info("Uin updated");
				String activeStatus = env.getProperty(ACTIVE_STATUS);
				if (activeStatus != null && activeStatus.equalsIgnoreCase(uinObject.getStatusCode())) {
					mosipLogger.info("Uin is in active status");
					notify(uin, true, request.getRequest().getRegistrationId());
				}
				return constructIdResponse(MOSIP_ID_UPDATE, service.retrieveIdentity(uinHash, IdType.UIN, null, null),
						null);
			} else {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES,
						NO_RECORD_FOUND.getErrorMessage());
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, UPDATE_IDENTITY, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}

	/**
	 * This function takes an individualId and an IdType as input and returns the
	 * RID in the
	 * form of a ResponseWrapper object
	 *
	 * @param individualId The ID of the individual whose RID is to be retrieved.
	 * @param idType       The type of ID that you're passing in.
	 * @return String
	 */
	@Override
	public String getRidByIndividualId(String individualId, IdType idType) throws IdRepoAppException {
		switch (idType) {
			case VID:
				individualId = getUinByVid(individualId);
			case UIN:
				individualId = retrieveRidByUin(individualId);
				return individualId;
			case ID:
				if (uinRepo.existsByRegId(individualId)) {
					return individualId;
				}
			default:
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "getRidByIndividualId",
						"NO_RECORD_FOUND");
				throw new IdRepoAppException(NO_RECORD_FOUND);
		}
	}

	/**
	 * It retrieves the RID of an individual by their UIN
	 *
	 * @param individualId The UIN of the individual
	 * @return The RID is being returned.
	 */
	private String retrieveRidByUin(String individualId) throws IdRepoAppException {
		String uinHash = retrieveUinHash(individualId);
		if (uinRepo.existsByUinHash(uinHash)) {
			return uinRepo.getRidByUinHash(uinHash);
		} else {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "retrieveRidByUin",
					"NO_RECORD_FOUND");
			throw new IdRepoAppException(NO_RECORD_FOUND);
		}
	}

	/**
	 * It takes a VID as input and returns the corresponding UIN
	 *
	 * @param vid Virtual ID
	 * @return The response is a map of key value pairs.
	 */
	private String getUinByVid(String vid) throws IdRepoDataValidationException, IdRepoAppException {
		try {
			RestRequestDTO request = restBuilder.buildRequest(RestServicesConstants.RETRIEVE_UIN_BY_VID, null,
					ResponseWrapper.class);
			request.setUri(request.getUri().replace("{vid}", vid));
			ResponseWrapper<Map<String, String>> response = restHelper.requestSync(request);
			return response.getResponse().get("UIN");
		} catch (RestServiceException e) {
			Optional<String> eBody = e.getResponseBodyAsString();
			if (eBody.isPresent()) {
				List<ServiceError> errorList = ExceptionUtils.getServiceErrorList(eBody.get());
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY,
						"\n" + errorList);
				throw new IdRepoAppException(errorList.get(0).getErrorCode(), errorList.get(0).getMessage());
			} else {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY,
						"\n" + e.getMessage());
				throw new IdRepoAppException(IdRepoErrorConstants.UNKNOWN_ERROR);
			}
		}
	}

	/**
	 * This function is used to get the maximum allowed update count of an attribute
	 * for the given individual id
	 *
	 * @param individualId  The UIN of the individual
	 * @param idType        The type of the ID. For example, UIN, RID, VID, etc.
	 * @param attributeList List of attributes for which the update count is to be
	 *                      retrieved.
	 * @return A map of attribute name and the maximum allowed update count for that
	 *         attribute.
	 */
	@Override
	public Map<String, Integer> getRemainingUpdateCountByIndividualId(String individualId, IdType idType,
																	  List<String> attributeList) throws IdRepoAppException {
		String uinHash = getUinHash(individualId, idType);
		return service.getRemainingUpdateCountByIndividualId(uinHash, idType,
				Objects.isNull(attributeList) ? List.of() : attributeList);
	}


	/**
	 * It takes in an individualId and an IdType, and returns the UIN hash of the
	 * individualId
	 *
	 * @param individualId The ID of the individual.
	 * @param idType       This is the type of the id that you are passing. It can
	 *                     be UIN, VID or RID.
	 * @return The UIN hash is being returned.
	 */
	private String getUinHash(String individualId, IdType idType)
			throws IdRepoDataValidationException, IdRepoAppException {
		
	   if (individualId != null && 
	    (individualId.toLowerCase().contains("@nin") || 
	     (idType != null && "HANDLE".equalsIgnoreCase(idType.name())))) {
	     return idRepoServiceHelper.getHandleHash(individualId);
		}
		switch (idType) {
			case VID:
				individualId = getUinByVid(individualId);
			case UIN:
				return retrieveUinHash(individualId);
			case ID:
				if (uinRepo.existsByRegId(individualId)) {
					return uinRepo.getUinHashByRid(individualId);
				}
			default:
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "getRidByIndividualId",
						"NO_RECORD_FOUND");
				throw new IdRepoAppException(NO_RECORD_FOUND);
		}
	}

	/**
	 * Construct id response.
	 *
	 * @param id               the id
	 * @param uin              the uin
	 * @param documents        the documents
	 * @return the id response DTO
	 * @throws IdRepoAppException the id repo app exception
	 */
	@SuppressWarnings("unchecked")
	private IdResponseDTO constructIdResponse(String id, Uin uin, List<DocumentsDTO> documents)
			throws IdRepoAppException {
		IdResponseDTO idResponse = new IdResponseDTO();
		idResponse.setId(id);
		idResponse.setVersion(EnvUtil.getAppVersion());
		ResponseDTO response = new ResponseDTO();
		response.setStatus(uin.getStatusCode());
		if (id.equals(this.id.get(READ))) {
			if (!Objects.isNull(documents)) {
				response.setDocuments(documents);
			}
			ObjectNode identityObject = convertToObject(uin.getUinData(), ObjectNode.class);
			response.setVerifiedAttributes(mapper.convertValue(identityObject.get("verifiedAttributes"), List.class));
			identityObject.remove("verifiedAttributes");
			constructAddressDetails(identityObject);
			removeNullNodes(identityObject);
			if (identityObject.get("NIN") != null) {
				String NIN = identityObject.get("NIN").asText();
				List<CardDetail> cardDetails = cardDetailRepository
						.getCardDetail(securityManager.hash(NIN.getBytes()));
				List<CardDetailDto> cardDetailDtos = new ArrayList<CardDetailDto>();
				if (!cardDetails.isEmpty()) {
					for (CardDetail cardDetail : cardDetails) {
						CardDetailDto cardDetailDto = new CardDetailDto();
						try {
							cardDetailDto.setDateOfExpiry(convertDate(cardDetail.getDateOfExpiry()));
							cardDetailDto.setDateOfIssuance(convertDate(cardDetail.getDateOfIssuance()));
						} catch (ParseException e) {
							throw new IdRepoAppException(PARSE_EXCEPTION);
						}

						cardDetailDto.setCardNumber(cardDetail.getCardNumber());
						cardDetailDtos.add(cardDetailDto);
					}
				}
				response.setCardDetails(cardDetailDtos);
			}

			response.setIdentity(identityObject);
		}
		idResponse.setResponse(response);
		return idResponse;
	}

	public static void removeNullNodes(ObjectNode objectNode) {

		List<String> nullKeys = new ArrayList<>();

		// Iterate through the fields of the ObjectNode
		Iterator<Entry<String, JsonNode>> fields = objectNode.fields();

		while (fields.hasNext()) {
			Entry<String, JsonNode> entry = fields.next();
			// Check if the value is null
			if (entry.getValue().isNull()) {
				nullKeys.add(entry.getKey()); // Collect the key
			}
		}

		// Now remove all the collected keys
		for (String key : nullKeys) {
			objectNode.remove(key);
		}
	}

	private void constructAddressDetails(ObjectNode identityObject) {

		JsonNode residenceStatus = identityObject
				.get(idRepoServiceHelper.getIdentityMapping().getIdentity().getResidenceStatus().getValue());
		JsonNode enrolmentStatus = identityObject
				.get(idRepoServiceHelper.getIdentityMapping().getIdentity().getEnrolmentStatus().getValue());
		JsonNode applicantOriginPlace = identityObject
				.get(idRepoServiceHelper.getIdentityMapping().getIdentity().getApplicantOriginPlace().getValue());
		JsonNode applicantBirthPlace = identityObject
				.get(idRepoServiceHelper.getIdentityMapping().getIdentity().getApplicantBirthPlace().getValue());
		JsonNode fatherResidence = identityObject
				.get(idRepoServiceHelper.getIdentityMapping().getIdentity().getFatherResidence().getValue());
		JsonNode fatherOrigin = identityObject
				.get(idRepoServiceHelper.getIdentityMapping().getIdentity().getFatherOrigin().getValue());
		JsonNode motherResidence = identityObject
				.get(idRepoServiceHelper.getIdentityMapping().getIdentity().getMotherResidence().getValue());
		JsonNode motherOrigin = identityObject
				.get(idRepoServiceHelper.getIdentityMapping().getIdentity().getMotherOrigin().getValue());
		if (residenceStatus != null && (residenceStatus.get(0).get("value").asText()).equalsIgnoreCase(OUTSIDEUGANDA)) {
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity().getApplicantPlaceOfResidenceCounty().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity().getApplicantPlaceOfResidenceSubCounty().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity().getApplicantPlaceOfResidenceDistrict().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity().getApplicantPlaceOfResidenceParish().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity().getApplicantPlaceOfResidenceVillage().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity().getApplicantPlaceOfResidenceStreet().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getApplicantPlaceOfResidenceYearsLived().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity().getApplicantPlaceOfResidenceDistrictOfPrevRes().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getApplicantPlaceOfResidenceHouseNo().getValue());
			identityObject
					.remove(idRepoServiceHelper.getIdentityMapping().getIdentity().getAppResCountryUGA().getValue());
			
		}
		if (residenceStatus != null && (residenceStatus.get(0).get("value").asText()).equalsIgnoreCase(INUGANDA)) {
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getApplicantForeignResidenceCountry().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getApplicantForeignResidenceAddress().getValue());
		}
		if (enrolmentStatus != null && (enrolmentStatus.get(0).get("value").asText()).equalsIgnoreCase(OUTSIDEUGANDA)) {
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getApplicantPlaceOfEnrolmentCounty().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getApplicantPlaceOfEnrolmentSubCounty().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getApplicantPlaceOfEnrolmentParish().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getApplicantPlaceOfEnrolmentVillage().getValue());
		}
		if (applicantBirthPlace != null
				&& (applicantBirthPlace.get(0).get("value").asText()).equalsIgnoreCase(OUTSIDEUGANDA)) {
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getAppBirCountryUGA().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getApplicantPlaceOfBirthCounty().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getApplicantPlaceOfBirthSubCounty().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getApplicantPlaceOfBirthDistrict().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getApplicantPlaceOfBirthParish().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getApplicantPlaceOfBirthVillage().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getApplicantPlaceOfBirthCity().getValue());
		}
		if (applicantBirthPlace != null
				&& (applicantBirthPlace.get(0).get("value").asText()).equalsIgnoreCase(INUGANDA)) {
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getApplicantForeignBirthCountry().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getApplicantForeignBirthAddress().getValue());
		}
		if (applicantOriginPlace != null
				&& (applicantOriginPlace.get(0).get("value").asText()).equalsIgnoreCase(OUTSIDEUGANDA)) {
			identityObject
					.remove(idRepoServiceHelper.getIdentityMapping().getIdentity().getAppOriCountryUGA().getValue());
			identityObject.remove(
					idRepoServiceHelper.getIdentityMapping().getIdentity().getApplicantPlaceOfOriginCounty()
							.getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getApplicantPlaceOfOriginSubCounty().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getApplicantPlaceOfOriginDistrict().getValue());
			identityObject.remove(
					idRepoServiceHelper.getIdentityMapping().getIdentity().getApplicantPlaceOfOriginParish()
							.getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getApplicantPlaceOfOriginVillage().getValue());
			identityObject.remove(
					idRepoServiceHelper.getIdentityMapping().getIdentity()
							.getApplicantPlaceOfOriginIndigenousCommunityTribe().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getApplicantPlaceOfOriginClan().getValue());
		}
		if (applicantOriginPlace != null
				&& (applicantOriginPlace.get(0).get("value").asText()).equalsIgnoreCase(INUGANDA)) {
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getApplicantForeignOriginAddress().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getApplicantForeignOriginCountry().getValue());
		}
		if (fatherResidence != null && (fatherResidence.get(0).get("value").asText()).equalsIgnoreCase(OUTSIDEUGANDA)) {
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getFatResCountryUGA().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getFatherPlaceOfResidenceDistrict().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getFatherPlaceOfResidenceCounty().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getFatherPlaceOfResidenceSubCounty().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getFatherPlaceOfResidenceParish().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getFatherPlaceOfResidenceVillage().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getFatherPlaceOfResidenceStreet().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getFatherPlaceOfResidenceHouseNo().getValue());

		}
		if (fatherResidence != null && (fatherResidence.get(0).get("value").asText()).equalsIgnoreCase(INUGANDA)) {
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getFatherForeignResidenceCountry().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getFatherForeignResidenceAddress().getValue());
		}
		if (fatherOrigin != null && (fatherOrigin.get(0).get("value").asText()).equalsIgnoreCase(OUTSIDEUGANDA)) {
			identityObject
					.remove(idRepoServiceHelper.getIdentityMapping().getIdentity().getFatOriCountryUGA().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getFatherPlaceOfResidenceDistrict().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getFatherPlaceOfResidenceCounty().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getFatherPlaceOfResidenceSubCounty().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getFatherPlaceOfResidenceParish().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getFatherPlaceOfResidenceVillage().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getFatherIndigenousCommunityTribe().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getFatherIndigenousCommunityClan().getValue());

		}
		if (fatherOrigin != null && (fatherOrigin.get(0).get("value").asText()).equalsIgnoreCase(INUGANDA)) {
			identityObject.remove(
					idRepoServiceHelper.getIdentityMapping().getIdentity().getFatherForeignOriginCountry().getValue());
			identityObject.remove(
					idRepoServiceHelper.getIdentityMapping().getIdentity().getFatherForeignOriginAddress().getValue());
		}
		if (motherResidence != null && (motherResidence.get(0).get("value").asText()).equalsIgnoreCase(OUTSIDEUGANDA)) {
			identityObject
					.remove(idRepoServiceHelper.getIdentityMapping().getIdentity().getMotResCountryUGA().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getMotherPlaceOfResidenceDistrict().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getMotherPlaceOfResidenceCounty().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getMotherPlaceOfResidenceSubCounty().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getMotherPlaceOfResidenceParish().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getMotherPlaceOfResidenceVillage().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getMotherPlaceOfResidenceStreet().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getMotherPlaceOfResidenceHouseNo().getValue());

		}
		if (motherResidence != null && (motherResidence.get(0).get("value").asText()).equalsIgnoreCase(INUGANDA)) {
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getMotherForeignResidenceCountry().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getMotherForeignResidenceAddress().getValue());
		}
		if (motherOrigin != null && (motherOrigin.get(0).get("value").asText()).equalsIgnoreCase(OUTSIDEUGANDA)) {
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity().getMotOriCountryUGA().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getMotherPlaceOfOriginDistrict().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getMotherPlaceOfOriginCounty().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getMotherPlaceOfOriginSubCounty().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getMotherPlaceOfOriginParish().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getMotherPlaceOfOriginVillage().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getMotherIndigenousCommunityTribe().getValue());
			identityObject.remove(idRepoServiceHelper.getIdentityMapping().getIdentity()
					.getMotherIndigenousCommunityClan().getValue());

		}
		if (motherOrigin != null && (motherOrigin.get(0).get("value").asText()).equalsIgnoreCase(INUGANDA)) {
			identityObject.remove(
					idRepoServiceHelper.getIdentityMapping().getIdentity().getMotherForeignOriginCountry().getValue());
			identityObject.remove(
					idRepoServiceHelper.getIdentityMapping().getIdentity().getMotherForeignOriginAddress().getValue());
		}
	}

	/**
	 * Convert Identity to object.
	 *
	 * @param identity the identity
	 * @param clazz    the clazz
	 * @return the object
	 * @throws IdRepoAppException the id repo app exception
	 */
	private <T> T convertToObject(byte[] identity, Class<T> clazz) throws IdRepoAppException {
		try {
			return mapper.readValue(identity, clazz);
		} catch (IOException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "convertToObject", e.getMessage());
			throw new IdRepoAppException(ID_OBJECT_PROCESSING_FAILED, e);
		}
	}
	private void notify(String uin,boolean isUpdate, String txnId) {
		try {
			sendGenericIdentityEvents(uin, isUpdate, txnId);
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "notify", e.getMessage());
		}
	}

	private EventModel createEventModel(String topic, Map<String, Object> eventData, String transactionId) {
		EventModel model = new EventModel();
		model.setPublisher(ID_REPO);
		String dateTime = DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime());
		model.setPublishedOn(dateTime);
		Event event = new Event();
		event.setTimestamp(dateTime);
		String eventId = UUID.randomUUID().toString();
		event.setId(eventId);
		event.setTransactionId(transactionId);
		Type type = new Type();
		type.setNamespace(idaEventTypeNamespace);
		type.setName(idaEventTypeName);
		event.setType(type);
		event.setData(eventData);
		model.setEvent(event);
		model.setTopic(topic);
		return model;
	}
	private void sendEventToWebsub(EventModel model) {
		try {
			mosipLogger.info(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "sendEventToWebsub",
					"Trying registering topic: " + model.getTopic());
			pb.registerTopic(model.getTopic(), env.getProperty(WEB_SUB_PUBLISH_URL));
		} catch (Exception e) {
			// Exception will be there if topic already registered. Ignore that
			mosipLogger.warn(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "sendEventToWebsub",
					"Error in registering topic: " + model.getTopic() + " : " + e.getMessage());
		}
		mosipLogger.info(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "sendEventToWebsub",
				"Publising event to topic: " + model.getTopic());
		pb.publishUpdate(model.getTopic(), model, MediaType.APPLICATION_JSON_VALUE, null,
				env.getProperty(WEB_SUB_PUBLISH_URL));
	}

	private void sendGenericIdentityEvents(String uin, boolean isUpdate, String registrationId) {
		mosipLogger.info("Inside sendGenericIdentityEvents");
		EventType eventType = isUpdate ? IDAEventType.IDENTITY_UPDATED : IDAEventType.IDENTITY_CREATED;
		Map<String, Object> eventData = new HashMap<>();
		eventData.put(ID_HASH, getIdHash(uin));
		eventData.put(REGISTRATION_ID, registrationId);
		String topic = eventType.toString();
		EventModel eventModel = createEventModel(topic, eventData, registrationId);
		mosipLogger.info(String.valueOf(eventModel));
		sendEventToWebsub(eventModel);
	}

	private String getIdHash(String uin) {
		int saltId = securityManager.getSaltKeyForId(uin);
		String hashSalt = uinHashSaltRepo.retrieveSaltById(saltId);
		return securityManager.hashwithSalt(uin.getBytes(), hashSalt.getBytes());
	}

	private IdResponseDTO retrieveIdentityByHandle(String handle, String type, Map<String, String> extractionFormats)
			throws IdRepoAppException {
		try {
			String handleHash = idRepoServiceHelper.getHandleHash(handle);
			Handle entity = handleRepo.findByHandleHash(handleHash);
			if (Objects.nonNull(entity)) {
				return retrieveIdentityByUinHash(type, entity.getUinHash(), extractionFormats);
			} else {
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY,
					"\n" + e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} catch (IdRepoAppException | IdRepoAppUncheckedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY,
					"\n" + e.getMessage());
			String errorCode = (e instanceof IdRepoAppException) ? ((IdRepoAppException) e).getErrorCode()
					: ((IdRepoAppUncheckedException) e).getErrorCode();
			String errorMsg = (e instanceof IdRepoAppException) ? ((IdRepoAppException) e).getErrorText()
					: ((IdRepoAppUncheckedException) e).getErrorText();
			throw new IdRepoAppException(errorCode, errorMsg, e);
		}
	}

	/**
	 * Exclusive-to-handle endpoint: returns every historical uin_h snapshot for
	 * the NIN-mapped uin_hash, with documents/biometrics reconstructed "as of"
	 * each snapshot's eff_dtimes from uin_document_h / uin_biometric_h.
	 *
	 * @param handle             the NIN handle (e.g. "cf192392613467@nin")
	 * @param type               all/bio/demo
	 * @param extractionFormats  optional biometric extraction formats
	 */
	public IdResponseHistoryDTO retrieveIdentityHistoryByHandle(String handle, String type,
																Map<String, String> extractionFormats) throws IdRepoAppException {
		try {
			String handleHash = idRepoServiceHelper.getHandleHash(handle);
			Handle handleEntity = handleRepo.findByHandleHash(handleHash);
			if (Objects.isNull(handleEntity)) {
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}

			String uinHash = handleEntity.getUinHash();
			List<UinHistory> historyList = uinHistoryRepo.findByUinHashOrderByEffectiveDateTimeDesc(uinHash);
			if (historyList.isEmpty()) {
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}

			List<HandleHistoryEntryDTO> entries = new ArrayList<>();
			for (UinHistory snapshot : historyList) {
				List<DocumentsDTO> documents = new ArrayList<>();
//				if (StringUtils.containsIgnoreCase(type, BIO) || StringUtils.containsIgnoreCase(type, ALL)) {
//					getBiometricFilesAsOf(snapshot, documents, extractionFormats);
//				}
				if (StringUtils.containsIgnoreCase(type, DEMO) || StringUtils.containsIgnoreCase(type, ALL)) {
					getDemographicFilesAsOf(snapshot, documents);
				}
				entries.add(constructHistoryEntry(snapshot, documents));
			}

			IdResponseHistoryDTO idResponse = new IdResponseHistoryDTO();
			idResponse.setId(this.id.get(READ));
			idResponse.setVersion(EnvUtil.getAppVersion());
			idResponse.setResponse(entries);
			return idResponse;

		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY,
					"\n" + e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		} catch (IdRepoAppException | IdRepoAppUncheckedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, RETRIEVE_IDENTITY,
					"\n" + e.getMessage());
			String errorCode = (e instanceof IdRepoAppException) ? ((IdRepoAppException) e).getErrorCode()
					: ((IdRepoAppUncheckedException) e).getErrorCode();
			String errorMsg = (e instanceof IdRepoAppException) ? ((IdRepoAppException) e).getErrorText()
					: ((IdRepoAppUncheckedException) e).getErrorText();
			throw new IdRepoAppException(errorCode, errorMsg, e);
		}
	}

	/**
	 * Builds one history entry (status/identity/documents/cardDetails) from a
	 * single uin_h snapshot row — mirrors the READ branch of constructIdResponse.
	 */
	private HandleHistoryEntryDTO constructHistoryEntry(UinHistory snapshot, List<DocumentsDTO> documents)
			throws IdRepoAppException {
		HandleHistoryEntryDTO entry = new HandleHistoryEntryDTO();
		entry.setStatus(snapshot.getStatusCode());
		entry.setEffectiveDateTime(DateUtils.formatToISOString(snapshot.getEffectiveDateTime()));

		if (!documents.isEmpty()) {
			entry.setDocuments(documents);
		}

		ObjectNode identityObject = convertToObject(snapshot.getUinData(), ObjectNode.class);
		entry.setVerifiedAttributes(mapper.convertValue(identityObject.get("verifiedAttributes"), List.class));
		identityObject.remove("verifiedAttributes");
		constructAddressDetails(identityObject);
		removeNullNodes(identityObject);

		if (identityObject.get("NIN") != null) {
			String nin = identityObject.get("NIN").asText();
			List<CardDetail> cardDetails = cardDetailRepository.getCardDetail(securityManager.hash(nin.getBytes()));
			List<CardDetailDto> cardDetailDtos = new ArrayList<>();
			for (CardDetail cardDetail : cardDetails) {
				CardDetailDto dto = new CardDetailDto();
				try {
					dto.setDateOfExpiry(convertDate(cardDetail.getDateOfExpiry()));
					dto.setDateOfIssuance(convertDate(cardDetail.getDateOfIssuance()));
				} catch (ParseException e) {
					throw new IdRepoAppException(PARSE_EXCEPTION);
				}
				dto.setCardNumber(cardDetail.getCardNumber());
				cardDetailDtos.add(dto);
			}
			entry.setCardDetails(cardDetailDtos);
		}

		entry.setIdentity(identityObject);
		return entry;
	}

	/**
	 * Demographic docs "as of" this snapshot: latest uin_document_h row per
	 * doccat_code with eff_dtimes <= snapshot.effDTimes.
	 */
	private void getDemographicFilesAsOf(UinHistory snapshot, List<DocumentsDTO> documents) {
		List<UinDocumentHistory> rows = uinDocHRepo.findAllUpToAsOf(snapshot.getUinRefId(), snapshot.getEffectiveDateTime());
		Map<String, UinDocumentHistory> latestPerCategory = new LinkedHashMap<>();
		for (UinDocumentHistory row : rows) {
			latestPerCategory.putIfAbsent(row.getDoccatCode(), row);
		}
		String uinHashUnsalted = snapshot.getUinHash().split("_")[1];
		latestPerCategory.values().forEach(demo -> {
			try {
				byte[] data = objectStoreHelper.getDemographicObject(uinHashUnsalted, demo.getDocId());
				if (demo.getDocHash().equals(securityManager.hash(data))) {
					documents.add(new DocumentsDTO(demo.getDoccatCode(), CryptoUtil.encodeToURLSafeBase64(data)));
				} else {
					throw new IdRepoAppException(DOCUMENT_HASH_MISMATCH);
				}
			} catch (IdRepoAppException e) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES, e.getMessage());
				throw new IdRepoAppUncheckedException(e.getErrorCode(), e.getErrorText(), e);
			}
		});
	}

	/**
	 * Biometric docs "as of" this snapshot: latest uin_biometric_h row per
	 * biometric_file_type with eff_dtimes <= snapshot.effDTimes.
	 */
	private void getBiometricFilesAsOf(UinHistory snapshot, List<DocumentsDTO> documents,
									   Map<String, String> extractionFormats) {
		List<UinBiometricHistory> rows = uinBioHRepo.findAllUpToAsOf(snapshot.getUinRefId(), snapshot.getEffectiveDateTime());
		Map<String, UinBiometricHistory> latestPerType = new LinkedHashMap<>();
		for (UinBiometricHistory row : rows) {
			latestPerType.putIfAbsent(row.getBiometricFileType(), row);
		}
		String uinHashUnsalted = snapshot.getUinHash().split("_")[1];
		latestPerType.values().forEach(bio -> {
			if (!allowedBioAttributes.contains(bio.getBiometricFileType())) {
				return;
			}
			try {
				byte[] data = objectStoreHelper.getBiometricObject(uinHashUnsalted, bio.getBioFileId());
				if (Objects.isNull(data)) {
					return;
				}
				if (Objects.nonNull(extractionFormats) && !extractionFormats.isEmpty()) {
					byte[] extracted = getBiometricsForRequestedFormats(uinHashUnsalted, bio.getBioFileId(),
							extractionFormats, data);
					if (Objects.nonNull(extracted)) {
						documents.add(new DocumentsDTO(bio.getBiometricFileType(),
								CryptoUtil.encodeToURLSafeBase64(extracted)));
					}
				} else if (StringUtils.equals(bio.getBiometricFileHash(), securityManager.hash(data))) {
					documents.add(new DocumentsDTO(bio.getBiometricFileType(), CryptoUtil.encodeToURLSafeBase64(data)));
				} else {
					throw new IdRepoAppException(DOCUMENT_HASH_MISMATCH);
				}
			} catch (IdRepoAppException e) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, GET_FILES, e.getMessage());
				throw new IdRepoAppUncheckedException(e.getErrorCode(), e.getErrorText(), e);
			}
		});
	}

	private List<BIR> filterExceptionBiometrics(List<BIR> birTypesForModality, List<BIR> finalBirs)
	{
		List<BIR> filteredBirs = new ArrayList<BIR>();
		for (BIR bir : birTypesForModality) {

		Map<String, String> othersMap = new HashMap<String, String>();
		if (bir.getOthers() != null) {
			othersMap = bir.getOthers().entrySet().stream()
					.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
		}

		if ((othersMap == null || !othersMap.containsKey("EXCEPTION")) ? true
				: !(Boolean.parseBoolean(othersMap.get("EXCEPTION")))) {
			filteredBirs.add(bir);
		} else {
			finalBirs.add(bir);
		}
	}
	return filteredBirs;
	}

	@WithRetry
	@Override
	public void updateCardNumber(Map<String, Object> data) {
		service.updateCardNumber(data);

	}

	private String convertDate(java.sql.Date date) throws ParseException {
		java.time.LocalDate localDate = date.toLocalDate();


		java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(dobFormat);

		// Format the LocalDate to a String in the "dd-MM-yyyy" format
		String formattedDate = localDate.format(formatter);

		return formattedDate;
	}
}

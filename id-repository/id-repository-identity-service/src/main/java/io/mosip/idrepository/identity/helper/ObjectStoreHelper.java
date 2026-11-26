package io.mosip.idrepository.identity.helper;

import static io.mosip.idrepository.core.constant.IdRepoConstants.BIO_DATA_REFID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DEMO_DATA_REFID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.OBJECT_STORE_ACCOUNT_NAME;
import static io.mosip.idrepository.core.constant.IdRepoConstants.OBJECT_STORE_ADAPTER_NAME;
import static io.mosip.idrepository.core.constant.IdRepoConstants.OBJECT_STORE_BUCKET_NAME;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.FILE_NOT_FOUND;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import io.mosip.commons.khazana.dto.ObjectDto;
import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.fsadapter.exception.FSAdapterException;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * @author Manoj SP
 *
 */
@Component
public class ObjectStoreHelper {
	
	@Value("${" + BIO_DATA_REFID + "}")
	private String bioDataRefId;
	
	@Value("${" + DEMO_DATA_REFID + "}")
	private String demoDataRefId;

	/** The Constant SLASH. */
	private static final String SLASH = "/";

	/** The Constant BIOMETRICS. */
	private static final String BIOMETRICS = "Biometrics";

	/** The Constant DEMOGRAPHICS. */
	private static final String DEMOGRAPHICS = "Demographics";

	@Value("${" + OBJECT_STORE_ACCOUNT_NAME + "}")
	private String objectStoreAccountName;

	@Value("${" + OBJECT_STORE_BUCKET_NAME + "}")
	private String objectStoreBucketName;

	@Value("${" + OBJECT_STORE_ADAPTER_NAME + "}")
	private String objectStoreAdapterName;

	/**
	 * Same flag the S3 adapter uses. When true the bucket is the account name and
	 * the object key is container/source/process/objectName, which is the only
	 * layout in which a UIN folder can be listed through the adapter.
	 */
	@Value("${object.store.s3.use.account.as.bucketname:false}")
	private boolean useAccountAsBucketName;

	/**
	 * Same S3 settings the Khazana S3 adapter reads. They are needed only to list
	 * a UIN folder by key prefix when the account is not used as the bucket name,
	 * because the adapter cannot list by prefix in that layout (it lists the whole
	 * bucket).
	 */
	@Value("${object.store.s3.url:}")
	private String s3Url;

	@Value("${object.store.s3.region:}")
	private String s3Region;

	@Value("${object.store.s3.accesskey:}")
	private String s3AccessKey;

	@Value("${object.store.s3.secretkey:}")
	private String s3SecretKey;

	@Value("${object.store.s3.bucket-name-prefix:}")
	private String s3BucketNamePrefix;

	private volatile AmazonS3 listingClient;

	private static final Logger mosipLogger = IdRepoLogger.getLogger(ObjectStoreHelper.class);

	
	private ObjectStoreAdapter objectStore;

	@Autowired
	public void setObjectStore(ApplicationContext context) {
		this.objectStore = context.getBean(objectStoreAdapterName, ObjectStoreAdapter.class);
	}

	/** The security manager. */
	@Autowired
	private IdRepoSecurityManager securityManager;

	public boolean demographicObjectExists(String uinHash, String fileRefId) {
		return exists(uinHash, false, fileRefId);
	}

	public boolean biometricObjectExists(String uinHash, String fileRefId) {
		return exists(uinHash, true, fileRefId);
	}

	public void putDemographicObject(String uinHash, String fileRefId, byte[] data) throws IdRepoAppException {
		putObject(uinHash, false, fileRefId, data, demoDataRefId);
	}

	public void putBiometricObject(String uinHash, String fileRefId, byte[] data) throws IdRepoAppException {
		putObject(uinHash, true, fileRefId, data, bioDataRefId);
	}

	public byte[] getDemographicObject(String uinHash, String fileRefId) throws IdRepoAppException {
		if (!this.demographicObjectExists(uinHash, fileRefId)) {
			throw new IdRepoAppException(FILE_NOT_FOUND);
		}
		return getObject(uinHash, false, fileRefId, demoDataRefId);
	}

	public byte[] getBiometricObject(String uinHash, String fileRefId) throws IdRepoAppException {
		if (!this.biometricObjectExists(uinHash, fileRefId)) {
			throw new IdRepoAppException(FILE_NOT_FOUND);
		}
		return getObject(uinHash, true, fileRefId, bioDataRefId);
	}

	/**
	 * Lists the objects stored under the UIN hash folder of the object store.
	 * With use.account.as.bucketname=true every file type under the folder is
	 * returned; otherwise only the Demographics folder is listed. Callers must
	 * filter the result by {@link ObjectDto#getSource()} /
	 * {@link ObjectDto#getProcess()}.
	 *
	 * Returns an empty list when the object store cannot be listed, so that
	 * retrieval of the current documents is never affected.
	 *
	 * @param uinHash the uin hash folder name
	 * @return the objects found, or an empty list
	 */
	public List<ObjectDto> listUinObjects(String uinHash) {
		try {
			if (useAccountAsBucketName) {
				String prefix = StringUtils.isBlank(objectStoreBucketName) ? uinHash
						: objectStoreBucketName + SLASH + uinHash;
				List<ObjectDto> objects = objectStore.getAllObjects(objectStoreAccountName, prefix);
				return objects == null ? Collections.emptyList() : objects;
			}
			return listUinFolderByPrefix(uinHash);
		} catch (RuntimeException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), "ObjectStoreHelper", "listUinObjects",
					"Unable to list objects of UIN folder: " + e.getMessage());
			return Collections.emptyList();
		}
	}

	/**
	 * Lists the Demographics folder of a UIN when objects are stored in a bucket
	 * named after the container (use.account.as.bucketname=false), where the key is
	 * <code>uinHash/Demographics/fileRefId</code>. Only that prefix is listed, so
	 * the cost does not depend on the size of the bucket.
	 */
	private List<ObjectDto> listUinFolderByPrefix(String uinHash) {
		String bucket = getListingBucketName();
		String prefix = uinHash + SLASH + DEMOGRAPHICS + SLASH;
		AmazonS3 client = getListingClient();
		List<ObjectDto> objects = new ArrayList<>();
		ListObjectsV2Request request = new ListObjectsV2Request().withBucketName(bucket).withPrefix(prefix);
		ListObjectsV2Result result;
		do {
			result = client.listObjectsV2(request);
			for (S3ObjectSummary summary : result.getObjectSummaries()) {
				String name = summary.getKey().substring(prefix.length());
				if (!name.isEmpty() && !name.contains(SLASH)) {
					objects.add(new ObjectDto(uinHash, DEMOGRAPHICS, name, summary.getLastModified()));
				}
			}
			request.setContinuationToken(result.getNextContinuationToken());
		} while (result.isTruncated());
		return objects;
	}

	/** Bucket name built the same way as the Khazana S3 adapter does. */
	private String getListingBucketName() {
		String bucket = objectStoreBucketName;
		if (StringUtils.isNotEmpty(s3BucketNamePrefix) && !bucket.startsWith(s3BucketNamePrefix)) {
			bucket = s3BucketNamePrefix + bucket;
		}
		return bucket.toLowerCase();
	}

	private AmazonS3 getListingClient() {
		AmazonS3 client = listingClient;
		if (client == null) {
			synchronized (this) {
				client = listingClient;
				if (client == null) {
					client = AmazonS3ClientBuilder.standard()
							.withCredentials(new AWSStaticCredentialsProvider(
									new BasicAWSCredentials(s3AccessKey, s3SecretKey)))
							.enablePathStyleAccess().withClientConfiguration(new ClientConfiguration())
							.withEndpointConfiguration(new EndpointConfiguration(s3Url, s3Region)).build();
					listingClient = client;
				}
			}
		}
		return client;
	}
	
	public void deleteBiometricObject(String uinHash, String fileRefId) {
		if (this.biometricObjectExists(uinHash, fileRefId)) {
			String objectName = uinHash + SLASH + BIOMETRICS + SLASH + fileRefId;
			objectStore.deleteObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName);
		}
	}

	private boolean exists(String uinHash, boolean isBio, String fileRefId) {
		String objectName = uinHash + SLASH + (isBio ? BIOMETRICS : DEMOGRAPHICS) + SLASH + fileRefId;
		return objectStore.exists(objectStoreAccountName, objectStoreBucketName, null, null, objectName);
	}

	private void putObject(String uinHash, boolean isBio, String fileRefId, byte[] data, String refId)
			throws IdRepoAppException {
		try {
			String objectName = uinHash + SLASH + (isBio ? BIOMETRICS : DEMOGRAPHICS) + SLASH + fileRefId;
			objectStore.putObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName,
					new ByteArrayInputStream(securityManager.encrypt(data, refId)));
		} catch (AmazonS3Exception | FSAdapterException e) {
			throw new IdRepoAppException(FILE_STORAGE_ACCESS_ERROR, e);
		}
	}

	private byte[] getObject(String uinHash, boolean isBio, String fileRefId, String refId) throws IdRepoAppException {
		try {
		String objectName = uinHash + SLASH + (isBio ? BIOMETRICS : DEMOGRAPHICS) + SLASH + fileRefId;
		return securityManager.decrypt(IOUtils.toByteArray(
				objectStore.getObject(objectStoreAccountName, objectStoreBucketName, null, null, objectName)), refId);
		} catch (AmazonS3Exception | FSAdapterException | IOException e) {
			throw new IdRepoAppException(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR);
		}
	}
}

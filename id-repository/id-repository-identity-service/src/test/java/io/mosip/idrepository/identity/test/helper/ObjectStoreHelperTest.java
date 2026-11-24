package io.mosip.idrepository.identity.test.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import io.mosip.commons.khazana.dto.ObjectDto;
import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.helper.ObjectStoreHelper;

@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest @Import(EnvUtil.class)
@ActiveProfiles("test")
public class ObjectStoreHelperTest {

	@InjectMocks
	private ObjectStoreHelper helper;

	@Mock
	private ObjectStoreAdapter adapter;

	@Mock
	private IdRepoSecurityManager securityManager;

	@Before
	public void init() {
		ReflectionTestUtils.setField(helper, "objectStoreAdapterName", "objectStoreAdapterName");
		ApplicationContext ctxMock = mock(ApplicationContext.class);
		when(ctxMock.getBean("objectStoreAdapterName", ObjectStoreAdapter.class)).thenReturn(adapter);
		helper.setObjectStore(ctxMock);
	}

	@Test
	public void testDemographicObjectExists() {
		when(adapter.exists(any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		boolean exists = helper.demographicObjectExists("", "");
		assertTrue(exists);
	}

	@Test
	public void testBiometricObjectExists() {
		when(adapter.exists(any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		boolean exists = helper.biometricObjectExists("", "");
		assertTrue(exists);
	}

	@Test
	public void testPutDemographicObject() throws IdRepoAppException {
		when(securityManager.encrypt(any(), any())).thenReturn("".getBytes());
		when(adapter.putObject(any(), any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		helper.putDemographicObject("hash", "refId", "".getBytes());
		ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
		verify(adapter).putObject(any(), any(), any(), any(), argCaptor.capture(), any());
		assertEquals("hash/Demographics/refId", argCaptor.getValue());
	}

	@Test
	public void testPutBiometricObject() throws IdRepoAppException {
		when(securityManager.encrypt(any(), any())).thenReturn("".getBytes());
		when(adapter.putObject(any(), any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		helper.putBiometricObject("hash", "refId", "".getBytes());
		ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
		verify(adapter).putObject(any(), any(), any(), any(), argCaptor.capture(), any());
		assertEquals("hash/Biometrics/refId", argCaptor.getValue());
	}

	@Test
	public void testPutObjectException() throws IdRepoAppException {
		when(securityManager.encrypt(any(), any())).thenReturn("".getBytes());
		when(adapter.putObject(any(), any(), any(), any(), any(), any())).thenThrow(new AmazonS3Exception(""));
		try {
			helper.putDemographicObject("hash", "refId", "".getBytes());
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testGetDemographicObject() throws IdRepoAppException {
		when(adapter.exists(any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		when(securityManager.decrypt(any(), any())).thenReturn("abc".getBytes());
		when(adapter.getObject(any(), any(), any(), any(), any()))
				.thenReturn(new ByteArrayInputStream("abc".getBytes()));
		byte[] demographicObject = helper.getDemographicObject("hash", "refId");
		assertEquals("abc", new String(demographicObject));
	}

	@Test
	public void testGetBiometricObject() throws IdRepoAppException {
		when(adapter.exists(any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		when(securityManager.decrypt(any(), any())).thenReturn("abc".getBytes());
		when(adapter.getObject(any(), any(), any(), any(), any()))
				.thenReturn(new ByteArrayInputStream("abc".getBytes()));
		byte[] bioObject = helper.getBiometricObject("hash", "refId");
		assertEquals("abc", new String(bioObject));
	}

	@Test
	public void testGetDemographicObjectNotFound() throws IdRepoAppException {
		when(adapter.exists(any(), any(), any(), any(), any())).thenReturn(Boolean.FALSE);
		try {
			helper.getDemographicObject("hash", "refId");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.FILE_NOT_FOUND.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.FILE_NOT_FOUND.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testGetBiometricObjectNotFound() throws IdRepoAppException {
		when(adapter.exists(any(), any(), any(), any(), any())).thenReturn(Boolean.FALSE);
		try {
			helper.getBiometricObject("hash", "refId");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.FILE_NOT_FOUND.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.FILE_NOT_FOUND.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testGetObjectException() throws IdRepoAppException {
		when(adapter.exists(any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		when(securityManager.encrypt(any(), any())).thenReturn("".getBytes());
		when(adapter.getObject(any(), any(), any(), any(), any())).thenThrow(new AmazonS3Exception(""));
		try {
			helper.getDemographicObject("hash", "refId");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testDeleteBiometricObject() throws IdRepoAppException {
		when(adapter.exists(any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		when(adapter.deleteObject(any(), any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		helper.deleteBiometricObject("hash", "refId");
		ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
		verify(adapter).deleteObject(any(), any(), any(), any(), argCaptor.capture());
		assertEquals("hash/Biometrics/refId", argCaptor.getValue());
	}

	@Test
	public void testDeleteBiometricObjectNotExists() throws IdRepoAppException {
		when(adapter.exists(any(), any(), any(), any(), any())).thenReturn(Boolean.FALSE);
		helper.deleteBiometricObject("hash", "refId");
	}

	@Test
	public void testListUinObjectsByPrefixWhenAccountIsNotBucket() {
		ReflectionTestUtils.setField(helper, "useAccountAsBucketName", false);
		ReflectionTestUtils.setField(helper, "objectStoreBucketName", "idrepo");
		AmazonS3 s3 = mock(AmazonS3.class);
		ReflectionTestUtils.setField(helper, "listingClient", s3);
		S3ObjectSummary current = new S3ObjectSummary();
		current.setKey("hash/Demographics/a.pdf");
		current.setLastModified(new Date());
		S3ObjectSummary folderMarker = new S3ObjectSummary();
		folderMarker.setKey("hash/Demographics/");
		ListObjectsV2Result page1 = new ListObjectsV2Result();
		page1.getObjectSummaries().add(current);
		page1.setTruncated(true);
		page1.setNextContinuationToken("t1");
		S3ObjectSummary older = new S3ObjectSummary();
		older.setKey("hash/Demographics/b.pdf");
		older.setLastModified(new Date());
		ListObjectsV2Result page2 = new ListObjectsV2Result();
		page2.getObjectSummaries().add(folderMarker);
		page2.getObjectSummaries().add(older);
		when(s3.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(page1, page2);
		List<ObjectDto> result = helper.listUinObjects("hash");
		assertEquals(2, result.size());
		assertEquals("a.pdf", result.get(0).getObjectName());
		assertEquals("b.pdf", result.get(1).getObjectName());
		assertEquals("Demographics", result.get(0).getProcess());
		ArgumentCaptor<ListObjectsV2Request> captor = ArgumentCaptor.forClass(ListObjectsV2Request.class);
		verify(s3, times(2)).listObjectsV2(captor.capture());
		assertEquals("idrepo", captor.getValue().getBucketName());
		assertEquals("hash/Demographics/", captor.getValue().getPrefix());
		verify(adapter, never()).getAllObjects(any(), any());
	}

	@Test
	public void testListUinObjectsByPrefixUsesBucketNamePrefixAndLowerCase() {
		ReflectionTestUtils.setField(helper, "useAccountAsBucketName", false);
		ReflectionTestUtils.setField(helper, "objectStoreBucketName", "IdRepo");
		ReflectionTestUtils.setField(helper, "s3BucketNamePrefix", "mosip-");
		AmazonS3 s3 = mock(AmazonS3.class);
		ReflectionTestUtils.setField(helper, "listingClient", s3);
		when(s3.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(new ListObjectsV2Result());
		assertTrue(helper.listUinObjects("hash").isEmpty());
		ArgumentCaptor<ListObjectsV2Request> captor = ArgumentCaptor.forClass(ListObjectsV2Request.class);
		verify(s3).listObjectsV2(captor.capture());
		assertEquals("mosip-idrepo", captor.getValue().getBucketName());
	}

	@Test
	public void testListUinObjectsByPrefixReturnsEmptyOnFailure() {
		ReflectionTestUtils.setField(helper, "useAccountAsBucketName", false);
		ReflectionTestUtils.setField(helper, "objectStoreBucketName", "idrepo");
		AmazonS3 s3 = mock(AmazonS3.class);
		ReflectionTestUtils.setField(helper, "listingClient", s3);
		when(s3.listObjectsV2(any(ListObjectsV2Request.class))).thenThrow(new AmazonS3Exception(""));
		assertTrue(helper.listUinObjects("hash").isEmpty());
	}

	@Test
	public void testListUinObjectsWithoutContainerPrefix() {
		ReflectionTestUtils.setField(helper, "useAccountAsBucketName", true);
		ReflectionTestUtils.setField(helper, "objectStoreAccountName", "idrepo");
		ReflectionTestUtils.setField(helper, "objectStoreBucketName", "");
		ObjectDto dto = new ObjectDto("Demographics", null, "file1.pdf", new Date());
		when(adapter.getAllObjects("idrepo", "hash")).thenReturn(Collections.singletonList(dto));
		List<ObjectDto> result = helper.listUinObjects("hash");
		assertEquals(1, result.size());
		assertEquals("file1.pdf", result.get(0).getObjectName());
	}

	@Test
	public void testListUinObjectsWithContainerPrefix() {
		ReflectionTestUtils.setField(helper, "useAccountAsBucketName", true);
		ReflectionTestUtils.setField(helper, "objectStoreAccountName", "idrepo");
		ReflectionTestUtils.setField(helper, "objectStoreBucketName", "idrepository");
		when(adapter.getAllObjects("idrepo", "idrepository/hash")).thenReturn(Collections.emptyList());
		assertTrue(helper.listUinObjects("hash").isEmpty());
		verify(adapter).getAllObjects("idrepo", "idrepository/hash");
	}

	@Test
	public void testListUinObjectsReturnsEmptyOnAdapterFailure() {
		ReflectionTestUtils.setField(helper, "useAccountAsBucketName", true);
		ReflectionTestUtils.setField(helper, "objectStoreAccountName", "idrepo");
		ReflectionTestUtils.setField(helper, "objectStoreBucketName", "");
		when(adapter.getAllObjects(any(), any())).thenThrow(new AmazonS3Exception(""));
		assertTrue(helper.listUinObjects("hash").isEmpty());
	}

}

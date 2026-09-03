package io.mosip.idrepository.identity.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.MGF1ParameterSpec;
import java.util.ArrayList;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource.PSpecified;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.manager.CredentialServiceManager;
import io.mosip.idrepository.core.manager.CredentialStatusManager;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.spi.IdRepoService;
import io.mosip.idrepository.core.spi.OnDemandCredentialService;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;

@Service
public class OnDemandCredentialServiceImpl implements OnDemandCredentialService {

    Logger mosipLogger = IdRepoLogger.getLogger(OnDemandCredentialServiceImpl.class);

    private static final String INDIVIDUAL_ID_TYPE = "individualIdType";

    private static final String INDIVIDUAL_ID = "individualId";

    private static final String MGF1 = "MGF1";

    private static final String HASH_ALGO = "SHA-256";

    @Value("${mosip.kernel.crypto.asymmetric-algorithm-name:RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING}")
    private String asymmetricAlgorithm;

    @Value("${mosip.idrepo.extraction.p12.filename:Partner.p12}")
    private String fileName;

    @Value("${mosip.idrepo.extraction.p12.password:partner@123}")
    private String cyptoPassword;

    @Value("${mosip.idrepo.extraction.p12.alias:partner}")
    private String alias;

    @Autowired
    private UinHashSaltRepo uinHashSaltRepo;

    @Autowired
    private CredentialServiceManager credentialServiceManager;

    @Autowired
    private CredentialStatusManager credentialStatusManager;

    @Autowired
    private IdRepoService<IdRequestDTO, IdResponseDTO> idRepoService;

    @Override
    public void issueCredential(Map<String, Object> data) {
        mosipLogger.info("Decrypting individual id");

        try {
            String id = decryptId((String)data.get(INDIVIDUAL_ID));

            String uin;
            if (IdType.HANDLE.name().equalsIgnoreCase((String)data.get(INDIVIDUAL_ID_TYPE))) {
                mosipLogger.info("Fetching uin for decrypted nin");
                IdResponseDTO response = idRepoService.retrieveIdentity(id, IdType.HANDLE, "metadata", null);
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> map = mapper.convertValue(response.getResponse().getIdentity(), Map.class);
                uin = (String)map.get("UIN");
            } else {
                uin = id;
            }

            if (uin == null) {
                mosipLogger.error("Uin not available, issuance failed");
                return;
            }

            mosipLogger.info("Notifying credential service for issuance");

            credentialServiceManager.notifyUinCredential(uin, null, null,
                    false, null,
                    uinHashSaltRepo::retrieveSaltById, credentialStatusManager::credentialRequestResponseConsumer,
                    null, new ArrayList<>(), null);

            mosipLogger.info("Notified credential service");
        } catch (IdRepoAppException e) {
            mosipLogger.error("Error while fetching uin from the nin: " + e.getMessage());
        } catch (Exception e) {
            mosipLogger.error("Error while issuing credential: " + e.getMessage());
        }
    }

    private String decryptId(String encryptedId) throws Exception {
        PrivateKeyEntry key = loadP12();
        byte[] decryptedData = asymmetricDecrypt(key.getPrivateKey(), CryptoUtil.decodePlainBase64(encryptedId));
        return new String(decryptedData);
    }

    public PrivateKeyEntry loadP12() throws Exception{
        try {
            KeyStore mosipKeyStore = KeyStore.getInstance("PKCS12");
            InputStream in = getClass().getClassLoader().getResourceAsStream(fileName);
            mosipKeyStore.load(in, cyptoPassword.toCharArray());
            ProtectionParameter password = new PasswordProtection(cyptoPassword.toCharArray());
            return (PrivateKeyEntry) mosipKeyStore.getEntry(alias, password);
        } catch (UnrecoverableEntryException | CertificateException | KeyStoreException | IOException| NoSuchAlgorithmException e) {
            mosipLogger.error("Error while loading p12: " + e.getMessage());
            throw e;
        }
    }

    private byte[] asymmetricDecrypt(PrivateKey privateKey, byte[] data) throws Exception{
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(asymmetricAlgorithm);
            OAEPParameterSpec oaepParams = new OAEPParameterSpec(HASH_ALGO, MGF1, MGF1ParameterSpec.SHA256,
                    PSpecified.DEFAULT);
            cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams);
            return doFinal(data, cipher);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new NoSuchAlgorithmException(e);
        } catch (InvalidKeyException e) {
            throw new InvalidKeyException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidAlgorithmParameterException(e);
        }
    }

    private byte[] doFinal(byte[] data, Cipher cipher) throws Exception {
        try {
            return cipher.doFinal(data);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            mosipLogger.error("Error while decrypting id: " + e.getMessage());
            throw e;
        }
    }

}
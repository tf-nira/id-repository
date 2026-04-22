package io.mosip.idrepository.core.util;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.kernel.core.logger.spi.Logger;

@Component
public class TokenIDGenerator {

	private static Logger mosipLogger = IdRepoLogger.getLogger(TokenIDGenerator.class);

	@Value("${mosip.kernel.tokenid.uin.salt}")
	private String uinSalt;

	@Value("${mosip.kernel.tokenid.length}")
	private int tokenIDLength;

	@Value("${mosip.kernel.tokenid.partnercode.salt}")
	private String partnerCodeSalt;

	public String generateTokenID(String uin, String partnerCode) {
		try {
			String uinWithSalt = uin + uinSalt;
			String uinHash = HMACUtils2.digestAsPlainText(uinWithSalt.getBytes());

			String partnerWithSalt = partnerCodeSalt + partnerCode;
			String hash = HMACUtils2.digestAsPlainText((partnerWithSalt + uinHash).getBytes());

			String token = new BigInteger(hash.getBytes()).toString().substring(0, tokenIDLength);

			mosipLogger.info("TOKEN_GEN", "TokenIDGenerator.generateTokenID", "GENERATING_TOKEN",
					"uin=" + uin +
							", uinSalt=" + uinSalt +
							", uinWithSalt=" + uinWithSalt +
							", uinHash=" + uinHash +
							", partnerCode=" + partnerCode +
							", partnerCodeSalt=" + partnerCodeSalt +
							", partnerWithSalt=" + partnerWithSalt +
							", hashInput=" + (partnerWithSalt + uinHash) +
							", finalToken=" + token);

			return token;
		} catch (NoSuchAlgorithmException e) {
			// TODO to be removed
			throw new IdRepoAppUncheckedException(IdRepoErrorConstants.UNKNOWN_ERROR, e);
		}
	}

}

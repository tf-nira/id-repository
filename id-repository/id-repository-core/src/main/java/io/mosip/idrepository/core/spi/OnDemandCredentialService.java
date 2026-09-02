package io.mosip.idrepository.core.spi;

import java.util.Map;

public interface OnDemandCredentialService {

    void issueCredential(Map<String, Object> data);
}
package io.mosip.credential.minio.cleanup.scheduler;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.credential.minio.cleanup.entity.CredentialEntity;
import io.mosip.credential.minio.cleanup.repositary.CredentialRepositary;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;

@Component
public class CredentialMinioCleanupScheduler {
	
    @Value("${credential.request.final.statuscodes}")
    private String statusCodes;
    
    @Autowired
    private CredentialRepositary<CredentialEntity, String> crdentialRepo;
    
    @Autowired
	ObjectStoreAdapter objectStoreAdapter;

	@Value("${credential.request.fetch.page.size:100}")
	private int pageSize;

	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialMinioCleanupScheduler.class);

	private static final String SCHEDULER = "CleanUpMinioScheduler";


	@Scheduled(cron = "${mosip.cleanup.minio.scheduler.cronexpression}")
    public void scheduleTask()
    {    try {
			LOGGER.info(IdRepoSecurityManager.getUser(), "CleanUpMinioScheduler", "scheuler started");
			String[] statusCodeArray = statusCodes.split(",");
		 if(statusCodeArray.length>0) {
				Sort sort = Sort.by(Sort.Direction.ASC, "createDateTime");
				Pageable pageable = PageRequest.of(0, pageSize, sort);
				 Page<CredentialEntity> pagecredentialEntities=crdentialRepo.findCredentialByStatusCodes(statusCodeArray,
							pageable);
				 if (pagecredentialEntities != null && pagecredentialEntities.getContent() != null && !pagecredentialEntities.getContent().isEmpty()) {
					 List<CredentialEntity> credentials=pagecredentialEntities.getContent();
					for (CredentialEntity credential : credentials) {
						String dataShareUrl = credential.getDataShareUrl();
						String[] dataShareArray = dataShareUrl.split("/");
						if (dataShareArray.length == 9) {
							 objectStoreAdapter.deleteObject(dataShareArray[7], dataShareArray[6], null,
							 null, dataShareArray[8]);
							LOGGER.info(IdRepoSecurityManager.getUser(), "CleanUpMinioScheduler",
									"credential = " + credential.getRequestId(),
									"Record deleted from minio " + dataShareArray[8]);

						}

					}
				}
			}
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), SCHEDULER,
					"error in scheduler " + ExceptionUtils.getStackTrace(e));
    }
		
		
    }
}

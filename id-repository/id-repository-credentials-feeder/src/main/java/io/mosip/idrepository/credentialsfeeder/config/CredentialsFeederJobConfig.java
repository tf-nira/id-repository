package io.mosip.idrepository.credentialsfeeder.config;

import static io.mosip.idrepository.credentialsfeeder.constant.Constants.DEFAULT_CHUNCK_SIZE;
import static io.mosip.idrepository.credentialsfeeder.constant.Constants.IDREPO_CREDENTIAL_FEEDER_CHUNK_SIZE;
import static io.mosip.idrepository.credentialsfeeder.constant.Constants.MOSIP_IDREPO_IDENTITY_UIN_STATUS_REGISTERED;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import io.mosip.idrepository.credentialsfeeder.entity.Uin;
import io.mosip.idrepository.credentialsfeeder.logger.IdRepoLogger;
import io.mosip.idrepository.credentialsfeeder.repository.UinRepo;
import io.mosip.idrepository.credentialsfeeder.step.CredentialsFeedingWriter;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;

/**
 * The Class CredentialsFeederJobConfig - provides configuration for Credentials
 * Feeder Job.
 *
 * @author Manoj SP
 */
@Configuration
@DependsOn({ "credentialsFeederConfig" })
public class CredentialsFeederJobConfig {

	private static final String CREDENTIALS_FEEDER = "CREDENTIALS_FEEDER";

	private static final Logger mosipLogger = IdRepoLogger.getLogger(CredentialsFeederJobConfig.class);

	@Value("${" + IDREPO_CREDENTIAL_FEEDER_CHUNK_SIZE + ":" + DEFAULT_CHUNCK_SIZE + "}")
	private int chunkSize;

	@Value("${" + MOSIP_IDREPO_IDENTITY_UIN_STATUS_REGISTERED + "}")
	private String uinActiveStatus;

	@Value("${idrepo.credential.feeder.instance3.from-date}")
	private String fromDateStr;
	
	@Value("${idrepo.credential.feeder.instance3.to-date:}")
	private String toDateStr;
	
	/**
	 * Job.
	 *
	 * @param step the step
	 * @return the job
	 */
	@Bean
	public Job job(Step step, JobBuilderFactory jobBuilderFactory, JobExecutionListener listener) {
		mosipLogger.info(CREDENTIALS_FEEDER, "CredentialsFeederJobConfig", "BUILDING JOB",
				"Building credentials feeder job with chunkSize: " + chunkSize);
		return jobBuilderFactory
				.get("job" + fromDateStr)  //job name updated based on instance
				.incrementer(new RunIdIncrementer())
				.listener(listener)
				.flow(step)
				.end()
				.build();
	}

	/**
	 * Step.
	 *
	 * @return the step
	 */
	@Bean
	public Step step(StepBuilderFactory stepBuilderFactory, CredentialsFeedingWriter writer, UinRepo uinRepo) {
		mosipLogger.info(CREDENTIALS_FEEDER, "CredentialsFeederJobConfig", "BUILDING STEP",
				"Building credentials feeder step with chunkSize: " + chunkSize
						+ " | reader: credentialEventReader"
						+ " | processor: asyncItemProcessor"
						+ " | writer: CredentialsFeedingWriter");
		return stepBuilderFactory
				.get("step")
				.<Uin, Future<Uin>>chunk(chunkSize)
				.reader(credentialEventReader(uinRepo))
				.processor(asyncItemProcessor())
				.writer(asyncItemWriter(writer))
				.build();
	}

	/**
	 * This function reads the data from the database and returns the data in the
	 * form of a list of
	 * objects
	 * 
	 * @param uinRepo This is the repository that we are using to fetch the data.
	 * @return A list of Uin objects
	 */
	@Bean
	public ItemReader<Uin> credentialEventReader(UinRepo uinRepo) {
		LocalDateTime fromDate;
		LocalDateTime effectiveToDate;
		
		try {
			if (fromDateStr == null || fromDateStr.isBlank()) {
				throw new DateTimeParseException("fromDateStr is null or blank", "", 0);
	        }
	        fromDate = LocalDateTime.parse(fromDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
			
		} catch (DateTimeParseException e) {
			 mosipLogger.error(CREDENTIALS_FEEDER, "CredentialsFeederJobConfig", "READER INIT",
		                "Invalid or missing fromDateStr: '" + fromDateStr + "' - " + e.getMessage());
		        throw new IllegalStateException(
		                "Credentials feeder job cannot start: invalid fromDateStr '" + fromDateStr + "'", e);
		}
		
		try {
	        effectiveToDate = (toDateStr != null && !toDateStr.isBlank())
	                ? LocalDateTime.parse(toDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
	                : DateUtils.getUTCCurrentDateTime();
	    } catch (DateTimeParseException e) {
	        mosipLogger.error(CREDENTIALS_FEEDER, "CredentialsFeederJobConfig", "READER INIT",
	                "Invalid toDateStr: '" + toDateStr + "' - " + e.getMessage());
	        throw new IllegalStateException(
	                "Credentials feeder job cannot start: invalid toDateStr '" + toDateStr + "'", e);
	    }
		
		mosipLogger.info(CREDENTIALS_FEEDER, "CredentialsFeederJobConfig", "READER INIT",
				"Initializing credentialEventReader"
						+ " | method: findByStatusCodeAndCreatedDateTimeBetween"
						+ " | uinActiveStatus: " + uinActiveStatus
						 + " | fromDate: " + fromDate
		                    + " | toDate: " + effectiveToDate
						+ " | pageSize/chunkSize: " + chunkSize
						+ " | sortBy: createdDateTime ASC");
		
		mosipLogger.info(CREDENTIALS_FEEDER, "CredentialsFeederJobConfig", "READER INIT",
	            "Initializing credentialEventReader"
	                    + " | fromDate: " + fromDate
	                    + " | toDate: " + effectiveToDate);

		RepositoryItemReader<Uin> reader = new RepositoryItemReader<>();
		reader.setRepository(uinRepo);
		reader.setMethodName("findByStatusCodeAndCreatedDateTimeBetween");
		reader.setArguments(List.of(uinActiveStatus, fromDate, effectiveToDate));
		final Map<String, Sort.Direction> sorts = new HashMap<>();
		sorts.put("createdDateTime", Direction.ASC); // then try processing Least failed entries first
		reader.setSort(sorts);
		reader.setPageSize(chunkSize);
		return reader;
	}

	/**
	 * The function creates an AsyncItemProcessor that delegates to the same
	 * function that it is passed
	 * 
	 * @return An AsyncItemProcessor
	 */
	@Bean
	public <T> AsyncItemProcessor<T, T> asyncItemProcessor() {
		AsyncItemProcessor<T, T> asyncItemProcessor = new AsyncItemProcessor<>();
		asyncItemProcessor.setDelegate(elem -> elem);
		asyncItemProcessor.setTaskExecutor(taskExecutor());
		return asyncItemProcessor;
	}

	/**
	 * The function takes an ItemWriter and returns an AsyncItemWriter that wraps
	 * the ItemWriter
	 * 
	 * @param itemWriter The ItemWriter that will be wrapped by the AsyncItemWriter.
	 * @return An AsyncItemWriter object.
	 */
	public <T> AsyncItemWriter<T> asyncItemWriter(ItemWriter<T> itemWriter) {
		AsyncItemWriter<T> asyncItemWriter = new AsyncItemWriter<>();
		asyncItemWriter.setDelegate(itemWriter);
		return asyncItemWriter;
	}

	/**
	 * Task executor.
	 *
	 * @return the task executor
	 */
	@Bean
	public TaskExecutor taskExecutor() {
		mosipLogger.info(CREDENTIALS_FEEDER, "CredentialsFeederJobConfig", "TASK EXECUTOR INIT",
				"Initializing ThreadPoolTaskExecutor"
						+ " | corePoolSize: " + chunkSize
						+ " | maxPoolSize: " + chunkSize
						+ " | queueCapacity: " + chunkSize
						+ " | threadNamePrefix: credential-feeder-");
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(chunkSize);
		executor.setMaxPoolSize(chunkSize);
		executor.setQueueCapacity(chunkSize);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.setThreadNamePrefix("credential-feeder-");
		return executor;
	}
}

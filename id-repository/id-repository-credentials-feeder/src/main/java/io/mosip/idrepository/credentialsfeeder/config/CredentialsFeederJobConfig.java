package io.mosip.idrepository.credentialsfeeder.config;

import static io.mosip.idrepository.credentialsfeeder.constant.Constants.DEFAULT_CHUNCK_SIZE;
import static io.mosip.idrepository.credentialsfeeder.constant.Constants.IDREPO_CREDENTIAL_FEEDER_CHUNK_SIZE;
import static io.mosip.idrepository.credentialsfeeder.constant.Constants.MOSIP_IDREPO_IDENTITY_UIN_STATUS_REGISTERED;

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
				.get("job")
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
				.<String, Future<String>>chunk(chunkSize)
				.reader(credentialEventReader(uinRepo))
				.processor(asyncItemProcessor())
				.writer(asyncItemWriter(writer))
				.build();
	}

	/**
	 * This function reads the data from the database and returns only the encrypted
	 * UIN string for each record. Using a String projection avoids loading the large
	 * {@code uinData} LOB column, which is not needed by the credentials feeder and
	 * would otherwise accumulate in memory across chunks.
	 *
	 * @param uinRepo This is the repository that we are using to fetch the data.
	 * @return A page of encrypted UIN strings
	 */
	@Bean
	public ItemReader<String> credentialEventReader(UinRepo uinRepo) {
		mosipLogger.info(CREDENTIALS_FEEDER, "CredentialsFeederJobConfig", "READER INIT",
				"Initializing credentialEventReader"
						+ " | method: findEncryptedUinByStatusCodeAndCreatedDateTimeBefore"
						+ " | uinActiveStatus: " + uinActiveStatus
						+ " | readBeforeDateTime: " + DateUtils.getUTCCurrentDateTime()
						+ " | pageSize/chunkSize: " + chunkSize
						+ " | sortBy: createdDateTime ASC");
		RepositoryItemReader<String> reader = new RepositoryItemReader<>();
		reader.setRepository(uinRepo);
		reader.setMethodName("findEncryptedUinByStatusCodeAndCreatedDateTimeBefore");
		reader.setArguments(List.of(uinActiveStatus, DateUtils.getUTCCurrentDateTime()));
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

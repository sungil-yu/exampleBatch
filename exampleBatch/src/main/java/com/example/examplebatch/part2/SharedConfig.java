package com.example.examplebatch.part2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;

@Profile("mysql")
@Configuration
@Slf4j
public class SharedConfig {



    @Bean
    public Job shareJob(JobRepository jobRepository, PlatformTransactionManager transactionManager){
        return new JobBuilder("shareJob")
                .incrementer(new RunIdIncrementer())
                .repository(jobRepository)
                .start(this.step(jobRepository, transactionManager))
                .next(this.step2(jobRepository, transactionManager))
                .build();
    }

    @Bean
    public Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager){
        return new StepBuilder("step")
                .repository(jobRepository)
                .transactionManager(transactionManager)
                .tasklet((contribution, chunkContext) -> {

                    StepExecution stepExecution = contribution.getStepExecution();
                    stepExecution.getExecutionContext().putString("stepKey", "step execution context");

                    JobExecution jobExecution = stepExecution.getJobExecution();
                    JobInstance jobInstance = jobExecution.getJobInstance();
                    jobExecution.getExecutionContext().putString("jobKey", "job execution context");

                    JobParameters jobParameters = jobExecution.getJobParameters();
                    jobParameters.getString("run.id");

                    log.info("jobName: {}", jobInstance.getJobName());
                    log.info("stepName: {}", stepExecution.getStepName());
                    log.info("parameter: {}", jobParameters.getString("run.id"));

                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step step2(JobRepository jobRepository, PlatformTransactionManager transactionManager){
        return new StepBuilder("step2")
                .repository(jobRepository)
                .transactionManager(transactionManager)
                .tasklet((contribution, chunkContext) -> {

                    StepExecution stepExecution = contribution.getStepExecution();
                    ExecutionContext stepExecutionContext = stepExecution.getExecutionContext();

                    JobExecution jobExecution = stepExecution.getJobExecution();
                    ExecutionContext jobExecutionContext = jobExecution.getExecutionContext();

                    log.info("jobKey: {}", jobExecutionContext.getString("jobKey", "emptyJobKey"));
                    log.info("stepKey: {}", stepExecutionContext.getString("stepKey", "emptyStepKey"));


                    return RepeatStatus.FINISHED;
                })
                .build();
    }


}

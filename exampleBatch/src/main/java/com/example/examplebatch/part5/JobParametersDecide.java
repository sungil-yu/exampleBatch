package com.example.examplebatch.part5;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class JobParametersDecide implements JobExecutionDecider {

    public static final FlowExecutionStatus CONTINUE = new FlowExecutionStatus("CONTINUE");

    private final String key;

    @Override
    public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {

        String key = jobExecution.getJobParameters().getString(this.key);

        if (!StringUtils.hasText(key)){
            return FlowExecutionStatus.COMPLETED;
        }

        return CONTINUE;
    }
}

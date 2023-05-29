package com.example.examplebatch.part4;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class LevelUpJobExecutionListener implements JobExecutionListener {

    private final UserRepository userRepository;

    @Override
    public void beforeJob(JobExecution jobExecution) {
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Collection<User> users = userRepository.findAllByUpdatedDate(LocalDate.now());

        long time = jobExecution.getEndTime().getTime() - jobExecution.getStartTime().getTime();

        log.info(" batch time : {}ms\", total data processed : {} user", time ,  users.size());
    }
}

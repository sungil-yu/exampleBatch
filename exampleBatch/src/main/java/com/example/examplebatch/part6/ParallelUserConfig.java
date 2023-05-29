package com.example.examplebatch.part6;

import com.example.examplebatch.part4.LevelUpJobExecutionListener;
import com.example.examplebatch.part4.SaveUserTasklet;
import com.example.examplebatch.part4.User;
import com.example.examplebatch.part4.UserRepository;
import com.example.examplebatch.part5.JobParametersDecide;
import com.example.examplebatch.part5.OrderStatistics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class ParallelUserConfig {


    private final StepBuilderFactory stepBuilderFactory;
    private final JobBuilderFactory jobBuilderFactory;
    private final UserRepository userRepository;
    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;
    private final int CHUNKSIZE = 1000;
    private final TaskExecutor taskExecutor;
    private final String JOB_NAME = "parallelUserJob";

    @Bean(JOB_NAME)
    public Job userJob() throws Exception {
        return this.jobBuilderFactory.get(JOB_NAME)
                .incrementer(new RunIdIncrementer())
                .listener(new LevelUpJobExecutionListener(userRepository)) // temp remove
                .start(this.saveUserFlow())
                .next(this.splitFlow(null))
                .build()
                .build();
    }

    @Bean(JOB_NAME + "_saveUserFlow")
    public Flow saveUserFlow() throws Exception {
        return new FlowBuilder<Flow>(JOB_NAME + "_saveUserFlow")
                .start(this.saveUserStep())
                .build();
    }


    @Bean(JOB_NAME + "_splitFlow")
    @JobScope
    public Flow splitFlow(@Value("#{jobParameters[date]}") String date) throws Exception {

        return new FlowBuilder<Flow>(JOB_NAME + "_splitFlow")
                .split(this.taskExecutor)
                .add(userLevelUpFlow(), orderStatisticsFlow(date))
                .build();


    }
    private Flow userLevelUpFlow() throws Exception {
        return new FlowBuilder<SimpleFlow>(JOB_NAME + "_userLevelUpFlow")
                .start(this.userLevelUpStep())
                .build();
    }

    private Step userLevelUpStep() throws Exception {
        return stepBuilderFactory.get(JOB_NAME + "_userLevelUpStep")
                .<User, User>chunk(CHUNKSIZE)
                .reader(itemReader())
                .processor(itemProcessor())
                .writer(itemWriter())
                .build();
    }


    private Flow orderStatisticsFlow(String data) throws Exception {
        return new FlowBuilder<SimpleFlow>(JOB_NAME+ "_orderStatisticsFlow")
                .start(new JobParametersDecide("date"))
                .on(JobParametersDecide.CONTINUE.getName())
                .to(this.orderStatisticsStep(data))
                .build();
    }

    private Step orderStatisticsStep(String date) throws Exception {
        return this.stepBuilderFactory.get(JOB_NAME + "_orderStatisticsStep")
                .<OrderStatistics, OrderStatistics>chunk(CHUNKSIZE)
                .reader(orderStatisticsItemReader(date))
                .writer(orderStatisticsItemWriter(date))
                .build();
    }

    private ItemWriter<? super OrderStatistics> orderStatisticsItemWriter(String date) throws Exception {

        YearMonth yearMonth = YearMonth.parse(date);
        String filename= yearMonth.getYear() + "년_" + yearMonth.getMonthValue() + "월_일별_주문_금액.csv";

        BeanWrapperFieldExtractor<OrderStatistics> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[]{"amount", "date"});

        DelimitedLineAggregator<OrderStatistics> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setDelimiter(",");
        lineAggregator.setFieldExtractor(fieldExtractor);

        FlatFileItemWriter<OrderStatistics> itemWriter = new FlatFileItemWriterBuilder<OrderStatistics>()
                .encoding("UTF-8")
                .resource(new FileSystemResource("output/" + filename))
                .lineAggregator(lineAggregator)
                .headerCallback(writer -> writer.write("total_amount, date"))
                .name(JOB_NAME + "_orderStatisticsItemWriter")
                .build();

        itemWriter.afterPropertiesSet();

        return itemWriter;
    }

    private ItemReader<? extends OrderStatistics> orderStatisticsItemReader(String date) throws Exception {
        YearMonth yearMonth = YearMonth.parse(date);

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("startDate", yearMonth.atDay(1));
        parameters.put("endDate", yearMonth.atEndOfMonth());

        Map<String, Order> sortKey = new HashMap<>();
        sortKey.put("created_date", Order.ASCENDING);

        JdbcPagingItemReader<OrderStatistics> itemReader = new JdbcPagingItemReaderBuilder<OrderStatistics>()
                .dataSource(dataSource)
                .rowMapper((rs, rowNum) -> OrderStatistics.builder()
                        .amount(rs.getString(1))
                        .date(LocalDate.parse(rs.getString(2), DateTimeFormatter.ISO_DATE))
                        .build())
                .pageSize(CHUNKSIZE)
                .name(JOB_NAME + "_orderStatisticsItemReader")
                .selectClause("sum(amount), created_date")
                .fromClause("orders")
                .whereClause("created_date >= :startDate and created_date <= :endDate")
                .groupClause("created_date")
                .parameterValues(parameters)
                .sortKeys(sortKey)
                .build();

        itemReader.afterPropertiesSet();

        return itemReader;
    }

    @Bean(JOB_NAME + "_saveUserStep")
    public Step saveUserStep() {
        return this.stepBuilderFactory.get(JOB_NAME + "_saveUserStep")
                .tasklet(new SaveUserTasklet(userRepository))
                .build();
    }


    private ItemWriter<? super User> itemWriter() {
        return users -> users.forEach(User::levelUp);
    }

    private ItemProcessor<? super User, ? extends User> itemProcessor() {
        return user -> {
            if (user.availableLevelUp()){
                return user;
            }

            return null;
        };

    }

    private ItemReader<? extends User> itemReader() throws Exception {
        JpaPagingItemReader<User> itemReader = new JpaPagingItemReaderBuilder<User>()
                .queryString("select u from User u")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNKSIZE)
                .name(JOB_NAME + "_userItemReader")
                .build();

        itemReader.afterPropertiesSet();

        return itemReader;
    }

}

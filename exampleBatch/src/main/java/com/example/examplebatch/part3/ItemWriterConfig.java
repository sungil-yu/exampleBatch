package com.example.examplebatch.part3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class ItemWriterConfig {


    private final StepBuilderFactory stepBuilderFactory;
    private final JobBuilderFactory jobBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;


    @Bean
    public Job itemWriterJob() throws Exception {
        return this.jobBuilderFactory.get("itemWriterJob")
                .incrementer(new RunIdIncrementer())
                .start(this.csvItemWriterStep())
//                .next(this.JdbcBatchItemWriterStep())
                .next(this.jpaItemWriterStep())
                .build();
    }

    @Bean
    public Step csvItemWriterStep() throws Exception {
        return this.stepBuilderFactory.get("csvItemWriterStep")
                .<Person, Person>chunk(10)
                .reader(itemReader())
                .writer(csvItemWriter())
                .build();
    }


    @Bean
    public Step JdbcBatchItemWriterStep() throws Exception {
        return this.stepBuilderFactory.get("JdbcBatchItemWriterStep")
                .<Person, Person>chunk(10)
                .reader(itemReader())
                .writer(JdbcBatchItemWriter())
                .build();
    }

    @Bean
    public Step jpaItemWriterStep() throws Exception {
        return this.stepBuilderFactory.get("jpaItemWriterStep")
                .<Person, Person>chunk(10)
                .reader(itemReader())
                .writer(jpaItemWriter())
                .build();
    }

    private ItemWriter<? super Person> jpaItemWriter() throws Exception {
        JpaItemWriter<Person> jpaItemWriter = new JpaItemWriterBuilder<Person>()
                .entityManagerFactory(entityManagerFactory)
                .usePersist(true)
                .build();

        jpaItemWriter.afterPropertiesSet();

        return jpaItemWriter;
    }

    private ItemWriter<? super Person> JdbcBatchItemWriter() {

        JdbcBatchItemWriter<Person> itemWriter = new JdbcBatchItemWriterBuilder<Person>()
                .dataSource(dataSource)
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql("insert into person(name, age, address) values(:name, :age, :address)")
                .build();

        itemWriter.afterPropertiesSet();

        return itemWriter;
    }

    private ItemWriter<? super Person> csvItemWriter() throws Exception {

        BeanWrapperFieldExtractor<Person> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[]{"id", "name", "age", "address"});

        DelimitedLineAggregator<Person> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setDelimiter(",");
        lineAggregator.setFieldExtractor(fieldExtractor);


        FlatFileItemWriter<Person> csvFileItemWriter = new FlatFileItemWriterBuilder<Person>()
                .name("csvFileItemWriter")
                .lineAggregator(lineAggregator)
                .encoding("UTF-8")
                .headerCallback(writer -> writer.write("id,이름,나이,거주지"))
                .footerCallback(writer -> writer.write("-------------------------------------------\n"))
                .resource(new FileSystemResource("output/test-out.csv"))
                .append(true)
                .build();

        csvFileItemWriter.afterPropertiesSet();

        return csvFileItemWriter;
    }
    private ItemReader<Person> itemReader() {
        return new CustomItemReader<>(getItems());
    }
    private List<Person> getItems() {
        List<Person> items = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            items.add(new Person( "test name" + i, "test age", "test address"));
        }
        return items;

    }


}

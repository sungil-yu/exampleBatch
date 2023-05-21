package com.example.examplebatch.part3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class ItemReaderConfiguration {


    private final StepBuilderFactory stepBuilderFactory;
    private final JobBuilderFactory jobBuilderFactory;

    private final DataSource dataSource;

    private static final int chunkSize = 10;

    public ItemReaderConfiguration(StepBuilderFactory stepBuilderFactory, JobBuilderFactory jobBuilderFactory, DataSource dataSource) {
        this.stepBuilderFactory = stepBuilderFactory;
        this.jobBuilderFactory = jobBuilderFactory;
        this.dataSource = dataSource;
    }


    @Bean
    public Job itemReaderJob() throws Exception {
        return this.jobBuilderFactory.get("itemReaderJob")
                .incrementer(new RunIdIncrementer())
                .start(this.customItemReaderStep())
                .next(this.csvFileItemReaderStep())
                .next(this.jdbcCursorItemReaderStep())
                .next(this.jdbcPagingItemReaderStep())
                .build();
    }



    private Step jdbcPagingItemReaderStep() throws Exception {
        return this.stepBuilderFactory.get("jdbcPagingItemReaderStep")
                .<Person, Person>chunk(chunkSize)
                .reader(jdbcPagingItemReader())
                .writer(itemWriter())
                .build();
    }

    private JdbcPagingItemReader<Person> jdbcPagingItemReader() throws Exception {

        JdbcPagingItemReader<Person> reader = new JdbcPagingItemReaderBuilder<Person>()
                .fetchSize(5)
                .pageSize(5)
                .dataSource(dataSource)
                .rowMapper(((rs, rowNum) -> new Person(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4))))
                .selectClause("id, name, age, address")
                .fromClause("person")
                .sortKeys(Map.of("id", Order.ASCENDING))
                .name("jdbcPagingItemReader")
                .build();

        reader.afterPropertiesSet();
        return reader;
    }

    private Step jdbcCursorItemReaderStep() {
        return this.stepBuilderFactory.get("jdbcCursorItemReaderStep")
                .<Person, Person>chunk(10)
                .reader(jdbcCursorItemReader())
                .writer(itemWriter())
                .build();
    }

    @Bean
    public Step customItemReaderStep() {
        return this.stepBuilderFactory.get("customItemReaderStep")
                .<Person, Person>chunk(10)
                .reader(new CustomItemReader<>(getItems()))
                .writer(itemWriter())
                .build();
    }


    @Bean
    public Step csvFileItemReaderStep() throws Exception {
        return this.stepBuilderFactory.get("csvFileItemReaderStep")
                .<Person, Person>chunk(10)
                .reader(csvFileItemReader())
                .writer(itemWriter())
                .build();
    }





    private JdbcCursorItemReader<Person> jdbcCursorItemReader() {
        return new JdbcCursorItemReaderBuilder<Person>()
                .fetchSize(10)
                .dataSource(dataSource)
                .rowMapper((rs, rowNum) -> new Person(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4)
                ))
                .sql("SELECT id, name, age, address FROM person")
                .name("jdbcCursorItemReader")
                .build();
    }


    private FlatFileItemReader<Person> csvFileItemReader() throws Exception {
        FlatFileItemReader<Person> csvFileItemReader = new FlatFileItemReader<>();
        csvFileItemReader.setEncoding("UTF-8");
        csvFileItemReader.setName("csvFileItemReader");
        csvFileItemReader.setResource(new ClassPathResource("test.csv"));
        csvFileItemReader.setLinesToSkip(1);
        csvFileItemReader.setLineMapper(lineMapper());

        csvFileItemReader.afterPropertiesSet();

        return csvFileItemReader;
    }

    private LineMapper<Person> lineMapper() {
        DefaultLineMapper<Person> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("id", "name", "age", "address");
        tokenizer.setDelimiter("/");
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSet -> {
            int id = fieldSet.readInt("id");
            String name = fieldSet.readString("name");
            String age = fieldSet.readString("age");
            String address = fieldSet.readString("address");

            return new Person(id, name, age, address);
        });


        return lineMapper;
    }

    private ItemWriter<Person> itemWriter() {
        return items -> log.info(items.stream()
                .map(Person::getName)
                .collect(Collectors.joining(", ")));
    }

    private List<Person> getItems() {
        List<Person> items = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            items.add(new Person(i + 1, "test name" + i, "test age", "test address"));
        }

        return items;
    }

}

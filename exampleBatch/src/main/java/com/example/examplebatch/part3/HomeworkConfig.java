package com.example.examplebatch.part3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class HomeworkConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;

    private final static Map<String, Person> persons = new ConcurrentHashMap<>();

    @Bean
    public Job homeworkJob() throws Exception {
        return this.jobBuilderFactory.get("homeworkJob")
                .incrementer(new RunIdIncrementer())
                .start(this.homeworkStep(null))
                .build();
    }

    @Bean
    @JobScope
    public Step homeworkStep(@Value("#{jobParameters[allow_duplicate]}") String allowDuplicate) throws Exception{

        return this.stepBuilderFactory.get("homeworkStep")
                .<Person, Person>chunk(10)
                .reader(csvFileReader())
                .processor(csvFileProcessor(Boolean.parseBoolean(allowDuplicate)))
                .writer(csvFileWriter())
                .build();

    }

    private ItemWriter<? super Person> csvFileWriter() {
        CompositeItemWriterBuilder<Person> writer = new CompositeItemWriterBuilder<>();
        writer.delegates(jdbcBatchItemWriter(), logItemWriter());
        return writer.build();
    }

    private ItemWriter<? super Person> logItemWriter() {
        return items -> items.stream().map(Person::getName).forEach(id -> log.info("PERSON.NAME : {}", id));
    }

    private ItemWriter<? super Person> jdbcBatchItemWriter() {

        JdbcBatchItemWriter<Person> itemWriter = new JdbcBatchItemWriterBuilder<Person>()
                .dataSource(dataSource)
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql("insert into person(name, age, address) values(:name, :age, :address)")
                .build();

        itemWriter.afterPropertiesSet();

        return itemWriter;
    }
    private ItemProcessor<? super Person, ? extends Person> csvFileProcessor(Boolean allowDuplicate) {

        return allowDuplicate ? item -> item : item -> {
            if(persons.containsKey(item.getName())) {
                return null;
            }
            persons.put(item.getName(), item);
            return item;
        };
    }

    private ItemReader<? extends Person> csvFileReader() throws Exception {

        FlatFileItemReaderBuilder<Person> flatFileReader = new FlatFileItemReaderBuilder<>();
        flatFileReader.name("csvFileReader");
        flatFileReader.encoding("UTF-8");
        flatFileReader.resource(new ClassPathResource("test.csv"));
        flatFileReader.linesToSkip(1);
        flatFileReader.lineMapper(lineMapper());
        FlatFileItemReader<Person> itemReader = flatFileReader.build();

        itemReader.afterPropertiesSet();

        return itemReader;

    }

    private LineMapper<Person> lineMapper() {

        DefaultLineMapper<Person> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("name", "age", "address");
        tokenizer.setDelimiter("/");
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSet -> {
            String name = fieldSet.readString("name");
            String age = fieldSet.readString("age");
            String address = fieldSet.readString("address");

            return new Person(name, age, address);
        });


        return lineMapper;
    }
}

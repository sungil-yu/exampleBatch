package com.example.examplebatch.part3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;


@Slf4j
public class PersonValidationRetryProcessor implements ItemProcessor<Person, Person> {

    private final RetryTemplate retryTemplate;

    public PersonValidationRetryProcessor() {
        this.retryTemplate = new RetryTemplateBuilder()
                .maxAttempts(3)
                .retryOn(NotFoundNameException.class)
                .withListener(new SavePersonRetryListener())
                .build();
    }

    @Override
    public Person process(Person person) throws Exception {

        return retryTemplate.execute(context -> {
            //retry callback 영향 (maxAttempts)
            if(person.isNotEmptyName()) {
                return person;
            }
            log.info("person name is empty.");

            throw new NotFoundNameException();
        }, context -> {
            //recovery callback

            log.info("recovery callback");
            return person.unknownName("UNKNOWN");
        });

    }

    public static class SavePersonRetryListener implements RetryListener{

        @Override
        public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
            log.info("open");
            return true;
        }

        @Override
        public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
            log.info("close");
        }

        @Override
        public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
            log.info("onError");
        }
    }
}

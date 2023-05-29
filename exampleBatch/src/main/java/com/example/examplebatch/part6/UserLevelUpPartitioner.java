package com.example.examplebatch.part6;

import com.example.examplebatch.part4.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class UserLevelUpPartitioner implements Partitioner {

    private final UserRepository userRepository;


    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        long minId = userRepository.findMinId();
        long maxId = userRepository.findMaxId();

        long targetSize = (maxId - minId) / gridSize + 1;


        Map<String, ExecutionContext> result = new HashMap<>();

        long stepNumber = 0;

        long itemStartIdx = minId;

        long itemEndIdx = itemStartIdx + targetSize - 1;

        while (itemStartIdx <= maxId){
            ExecutionContext executionContext = new ExecutionContext();
            result.put("partition" + stepNumber, executionContext);

            if(itemEndIdx >= maxId){
                itemEndIdx = maxId;
            }

            executionContext.putLong("minId", itemStartIdx);
            executionContext.putLong("maxId", itemEndIdx);

            itemStartIdx += targetSize;
            itemEndIdx += targetSize;

            stepNumber++;
        }
        return result;
    }
}

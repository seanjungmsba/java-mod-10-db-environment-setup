package com.example.restservice.controller;

import com.example.restservice.models.Counter;
import com.example.restservice.models.Greeting;
import com.example.restservice.models.Haystack;
import com.example.restservice.models.HaystackUUID;
import com.example.restservice.repositories.CounterRepository;
import com.example.restservice.repositories.HaystackRepository;
import com.example.restservice.repositories.HaystackUUIDRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static java.util.UUID.randomUUID;

@RestController
public class GreetingController {

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();
    @Autowired
    private CounterRepository counterRepository;
    @Autowired
    private HaystackRepository haystackRepository;

//    @GetMapping("/persistent_greeting")
//    public Greeting persistentGreeting(@RequestParam(value = "name", defaultValue = "World") String name) {
//        Counter persistentCounter = counterRepository.findById(1L).orElseGet(() -> new Counter(0L));
//        Long persistentCounterValue = persistentCounter.incrementAndGet();
//        counterRepository.save(persistentCounter);
//        return new Greeting(persistentCounterValue, String.format(template, name));
//    }
    @Autowired
    private HaystackUUIDRepository haystackUUIDRepository;

    @GetMapping("/greeting")
    public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        return new Greeting(counter.incrementAndGet(), String.format(template, name));
    }

    @GetMapping("/set_count")
    public Greeting setCount(@RequestParam(value = "value", defaultValue = "0") Long value) {
        Counter newCounter = counterRepository.findById(1L).orElseGet(() -> new Counter(0L));
        newCounter.setCounter(value);
        counterRepository.save(newCounter);
        return new Greeting(newCounter.getCounter(), "Count value set.");
    }

    @GetMapping("/get_count")
    public Greeting getCount() {
        Counter newCounter = counterRepository.findById(1L).orElseGet(() -> new Counter(0L));
        return new Greeting(newCounter.getCounter(), "Count value get.");
    }

    @GetMapping("/get_count_trace")
    public Greeting getCountTrace() {
        String counterRepositoryOutput = counterRepository.getCounterAndTrace();
        return new Greeting(Long.parseLong(counterRepositoryOutput.split(",")[1], 10),
                String.format("Count value get, on instance %s",
                        counterRepositoryOutput.split(",")[2]));
    }

    @GetMapping("/set_count_trace")
    public Greeting setCountTrace(@RequestParam(value = "value", defaultValue = "0") Long value) {
        Counter newCounter = counterRepository.findById(1L).orElseGet(() -> new Counter(0L));
        String counterRepositoryOutput = counterRepository.setCounterAndTrace(value);
        return new Greeting(Long.parseLong(counterRepositoryOutput.split(",")[1], 10),
                String.format("Count value set, on instance %s",
                        counterRepositoryOutput.split(",")[2]));
    }

    @GetMapping("/benchmark")
    public String benchmark(@RequestParam(value = "count", defaultValue = "1000") long count) {
        long i = 1L;

        // flush tables
        haystackRepository.truncateTable();
        haystackUUIDRepository.truncateTable();

        // TODO: Initialize Tables
        while (i < count) {
            // get random uuid
            UUID uuid = randomUUID();

            // generate new Haystack and HaystackUUID objects using uuid
            Haystack haystack = new Haystack(uuid, "hay");
            HaystackUUID haystackUUID = new HaystackUUID(uuid);

            // save objects to their respective Database tables
            haystackRepository.save(haystack);
            haystackUUIDRepository.save(haystackUUID);
            i++;
        }

        // generate and save needle object at end
        UUID uuid = randomUUID();
        haystackRepository.save(new Haystack(uuid, "needle"));
        haystackUUIDRepository.save(new HaystackUUID(uuid));

        // query1: Basic query with sequential scan
        Haystack query1 = haystackRepository.seqScan();
        String query1Perf = haystackRepository.seqScanPerf().stream().collect(Collectors.joining("\n"));

        // query2: Table join
        Haystack query2 = haystackRepository.tableJoin();
        String query2Perf = haystackRepository.tableJoinPerf().stream().collect(Collectors.joining("\n"));

        // query3: Basic query with indexing
        haystackRepository.indexHash();
        Haystack query3 = haystackRepository.seqScan();
        String query3Perf = haystackRepository.seqScanPerf().stream().collect(Collectors.joining("\n"));

        // query4: Table join with indexing
        haystackUUIDRepository.indexHash();
        Haystack query4 = haystackRepository.tableJoin();
        String query4Perf = haystackRepository.tableJoinPerf().stream().collect(Collectors.joining("\n"));

        return query1.toString() +
                query1Perf + "\n---------------------------------------\n" +
                query2.toString() +
                query2Perf + "\n---------------------------------------\n" +
                query3.toString() +
                query3Perf + "\n---------------------------------------\n" +
                query4.toString() +
                query4Perf;
    }


}

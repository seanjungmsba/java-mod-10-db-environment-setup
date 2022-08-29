package com.example.restservice.repositories;

import com.example.restservice.models.Counter;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CounterRepository extends CrudRepository<Counter, Long> {
    @Query(
            value = "SELECT id, count, cast(inet_server_addr() as text) FROM counter WHERE id = 1",
            nativeQuery = true)
    String getCounterAndTrace();

    @Query(
            value = "UPDATE counter SET count = ?1 WHERE id = 1 RETURNING id, count, cast(inet_server_addr() as text)",
            nativeQuery = true)
    String setCounterAndTrace(Long count);
}

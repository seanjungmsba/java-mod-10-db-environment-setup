package com.example.restservice.repositories;

import com.example.restservice.models.Haystack;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface HaystackRepository extends CrudRepository<Haystack, Long> {
    @Transactional
    @Modifying
    @Query(
            value = "TRUNCATE haystack; DROP INDEX IF EXISTS haystack_value_idx",
            nativeQuery = true)
    void truncateTable();

    @Transactional
    @Modifying
    @Query(
            value = "CREATE INDEX ON haystack (value)",
            nativeQuery = true)
    void indexBTree();

    @Transactional
    @Modifying
    @Query(
            // TODO: write query to create hash index on value column
            value = "CREATE INDEX ON haystack USING HASH (value)",
            nativeQuery = true)
    void indexHash();

    @Query(
            // TODO: return full row when the value column is a needle
            value = "SELECT * FROM haystack WHERE value = 'needle'",
            nativeQuery = true)
    Haystack seqScan();

    @Query(
            // TODO: return the performance metrics from running the seqScan query
            value = "EXPLAIN ANALYZE SELECT * FROM haystack WHERE value = 'needle'",
            nativeQuery = true)
    List<String> seqScanPerf();

    @Query(
            // TODO: return values of id, uuid, and value from an inner join between haystack and haystackuuid tables when the value column is a needle
            value = "SELECT haystack.id, haystack.uuid, haystack.value FROM haystack INNER JOIN haystackuuid ON haystack.uuid = haystackuuid.uuid WHERE value = 'needle'",
            nativeQuery = true)
    Haystack tableJoin();

    @Query(
            // TODO: return the performance metrics from running the tableJoin query
            value = "EXPLAIN ANALYZE SELECT * FROM haystack INNER JOIN haystackuuid ON haystack.uuid = haystackuuid.uuid WHERE value = 'needle'",
            nativeQuery = true)
    List<String> tableJoinPerf();
}


package com.example.restservice.models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Counter {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long count;

    protected Counter() {}

    public Counter(Long count) {
        this.count = count;
    }

    public Long incrementAndGet() {
        this.count += 1;
        return count;
    }

}

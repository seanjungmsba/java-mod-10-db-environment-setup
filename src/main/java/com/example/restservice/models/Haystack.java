package com.example.restservice.models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import java.util.UUID;


@Entity
public class Haystack {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    private UUID uuid;
    private String value;

    protected Haystack() {}

    public Haystack(UUID uuid, String value) {
        this.uuid = uuid;
        this.value = value;
    }

    public String toString() {
        return String.format("Needle has value %s at id %s\n", uuid, id);
    }

}


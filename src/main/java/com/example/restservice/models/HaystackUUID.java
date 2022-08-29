package com.example.restservice.models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.UUID;

@Entity
public class HaystackUUID {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private UUID uuid;

    protected HaystackUUID() {
    }

    public HaystackUUID(UUID uuid) {
        this.uuid = uuid;
    }

}

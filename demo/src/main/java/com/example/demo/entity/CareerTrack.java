package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "career_tracks")
public class CareerTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private int levels;

    @Column(nullable = false)
    private int branches;

    @Column(columnDefinition = "TEXT")
    private String branchNamesJson;

    @Column(columnDefinition = "TEXT")
    private String levelNamesJson;   // JSON объект: {"Frontend": ["Junior", "Middle"], "Backend": [...]}
}

package com.example.demo.entity;

import jakarta.persistence.*;

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
    private String levelNamesJson;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getLevels() { return levels; }
    public void setLevels(int levels) { this.levels = levels; }

    public int getBranches() { return branches; }
    public void setBranches(int branches) { this.branches = branches; }

    public String getBranchNamesJson() { return branchNamesJson; }
    public void setBranchNamesJson(String branchNamesJson) { this.branchNamesJson = branchNamesJson; }

    public String getLevelNamesJson() { return levelNamesJson; }
    public void setLevelNamesJson(String levelNamesJson) { this.levelNamesJson = levelNamesJson; }
}

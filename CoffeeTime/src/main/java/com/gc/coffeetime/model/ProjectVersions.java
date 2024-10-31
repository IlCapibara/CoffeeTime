package com.gc.coffeetime.model;

import java.util.List;

public class ProjectVersions {
    private String projectName;
    private List<String> versions;

    public ProjectVersions(String projectName, List<String> versions) {
        this.projectName = projectName;
        this.versions = versions;
    }

    // Getters and Setters
    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public List<String> getVersions() {
        return versions;
    }

    public void setVersions(List<String> versions) {
        this.versions = versions;
    }

    @Override
    public String toString() {
        return "Project: " + projectName + ", Versions: " + versions;
    }
}

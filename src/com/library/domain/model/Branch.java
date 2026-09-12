package com.library.domain.model;

import java.util.Objects;

/**
 * A physical library branch. Copies belong to a branch, which is what makes
 * multi-branch inventory and transfers possible.
 */
public class Branch {

    private final String branchId;
    private String name;
    private String city;

    public Branch(String branchId, String name, String city) {
        this.branchId = branchId;
        this.name = name;
        this.city = city;
    }

    public String getBranchId() {
        return branchId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return Objects.equals(branchId, ((Branch) other).branchId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(branchId);
    }

    @Override
    public String toString() {
        return String.format("%-8s | %-24s | %s", branchId, name, city);
    }
}

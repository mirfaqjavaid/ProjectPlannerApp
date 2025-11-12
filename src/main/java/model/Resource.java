package model;

import java.util.*;

public class Resource {
    private String name;
    private List<Allocation> allocations;

    public Resource(String name, List<Allocation> allocations) {
        this.name = name;
        this.allocations = allocations != null ? allocations : new ArrayList<>();
    }

    
    public String getName() { return name; }
    public List<Allocation> getAllocations() { return allocations; }
    public void setName(String name) { this.name = name; }
    public void setAllocations(List<Allocation> allocations) { this.allocations = allocations; }

    @Override
    public String toString() {
        return name + " " + allocations;
    }
}
package com.cc.job.test.entity;

import java.util.Objects;

public class Edge {

    private Integer startId;

    private Integer endId;

    public Edge() {
    }

    public Edge(Integer startId, Integer endId) {
        this.startId = startId;
        this.endId = endId;
    }

    public Integer getStartId() {
        return startId;
    }

    public void setStartId(Integer startId) {
        this.startId = startId;
    }

    public Integer getEndId() {
        return endId;
    }

    public void setEndId(Integer endId) {
        this.endId = endId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return Objects.equals(startId, edge.startId) &&
                Objects.equals(endId, edge.endId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startId, endId);
    }

    @Override
    public String toString() {
        return "Edge{" +
                "startId=" + startId +
                ", endId=" + endId +
                '}';
    }
}

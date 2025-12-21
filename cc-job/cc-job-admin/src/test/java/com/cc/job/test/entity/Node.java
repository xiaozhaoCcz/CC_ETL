package com.cc.job.test.entity;

import java.util.Objects;

public class Node {

    private Integer id;

    private String nodeName;

    private Integer outCount;

    private Integer inCount;

    public Node() {
    }

    public Node(Integer id, String nodeName, Integer outCount, Integer inCount) {
        this.id = id;
        this.nodeName = nodeName;
        this.outCount = outCount;
        this.inCount = inCount;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public Integer getOutCount() {
        return outCount;
    }

    public void setOutCount(Integer outCount) {
        this.outCount = outCount;
    }

    public Integer getInCount() {
        return inCount;
    }

    public void setInCount(Integer inCount) {
        this.inCount = inCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return Objects.equals(id, node.id) &&
                Objects.equals(nodeName, node.nodeName) &&
                Objects.equals(outCount, node.outCount) &&
                Objects.equals(inCount, node.inCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nodeName, outCount, inCount);
    }

    @Override
    public String toString() {
        return "Node{" +
                "id=" + id +
                ", nodeName='" + nodeName + '\'' +
                ", outCount=" + outCount +
                ", inCount=" + inCount +
                '}';
    }
}

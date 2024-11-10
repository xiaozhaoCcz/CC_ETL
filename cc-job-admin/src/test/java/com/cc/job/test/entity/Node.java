package com.cc.job.test.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString
public class Node {

    private Integer id;

    private String nodeName;

    private Integer outCount;

    private Integer inCount;
}

package com.example.toolxml.mybatis.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestCase {
    private Integer id;
    private String caseName;
    private String caseSign;
    private String caseDescription;
    private String fileName;
    private String filePath;
    private Boolean isRightCase;
    private LinkedHashMap<String, String> valueList;
}

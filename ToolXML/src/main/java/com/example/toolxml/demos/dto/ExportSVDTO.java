package com.example.toolxml.demos.dto;

import com.example.toolxml.mybatis.entity.DataFrame;
import com.example.toolxml.mybatis.entity.TestCase;
import lombok.Data;

import java.util.List;

@Data
public class ExportSVDTO {
    List<DataFrame> frameList;
    List<TestCase> caseList;
    Integer caseId;
}

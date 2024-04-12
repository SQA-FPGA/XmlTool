package com.example.toolxml.demos.excel;

import cn.afterturn.easypoi.excel.annotation.Excel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class TestCaseExcel {
    @Excel(name = "序号")
    private Integer id;
    @Excel(name = "用例名称")
    private String caseName;
    @Excel(name = "用例标识")
    private String caseSign;
    @Excel(name = "用例描述")
    private String caseDescription;
    @Excel(name = "对应sv文件")
    private String fileName;
    @Excel(name = "对应sv文件路径")
    private String filePath;
}

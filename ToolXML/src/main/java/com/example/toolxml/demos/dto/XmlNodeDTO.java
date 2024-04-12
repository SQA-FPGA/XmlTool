package com.example.toolxml.demos.dto;

import lombok.Data;

import java.util.List;

@Data
public class XmlNodeDTO {
    private String nodeName;
    private String dataType;
    private String dataLength;
    private List<String> valueNames;
    private String valueName;
    private List<DataContentDTO> dataContent;
}

package com.example.toolxml.mybatis.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataFrame {
    private Integer id;
    private String frameHeader;
    private String dataLength;
    private String instructionType;
    private String instructionContent1;
    private String instructionContent2;
    private String instructionContent3;
    private String instructionContent4;
    private String instructionContent5;
    private String instructionContent6;
    private String instructionContent7;
    private String instructionContent8;
    private String checksum;
    private Boolean isRightCase;
}

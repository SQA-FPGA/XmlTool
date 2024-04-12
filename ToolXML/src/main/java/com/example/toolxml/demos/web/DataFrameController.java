package com.example.toolxml.demos.web;

import com.example.toolxml.demos.dto.DataContentDTO;
import com.example.toolxml.demos.dto.ExportSVDTO;
import com.example.toolxml.demos.dto.XmlNodeDTO;
import com.example.toolxml.demos.excel.TestCaseExcel;
import com.example.toolxml.demos.utils.ExcelUtils;
import com.example.toolxml.mybatis.entity.DataFrame;
import com.example.toolxml.mybatis.entity.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class DataFrameController {
    private static Map<String, String> fieldMappings = new HashMap<String, String>() {{
        put("帧头", "frameHeader");
        put("数据长度", "dataLength");
        put("指令类型", "instructionType");
        put("指令内容1", "instructionContent1");
        put("指令内容2", "instructionContent2");
        put("指令内容3", "instructionContent3");
        put("指令内容4", "instructionContent4");
        put("指令内容5", "instructionContent5");
        put("指令内容6", "instructionContent6");
        put("指令内容7", "instructionContent7");
        put("校验和", "checksum");
    }};

    @GetMapping("/form")
    public String showUploadForm() {
        return "upload-form";
    }

    @PostMapping("/upload")
    public String uploadXmlFile(@RequestParam("file") MultipartFile file, Model model) {
        List<DataFrame> frameList = new ArrayList<>();
        List<TestCase> caseList = new ArrayList<>();
        List<XmlNodeDTO> xmlNodeDTOList = new ArrayList<>();

        if (!file.isEmpty()) {
            try (InputStream inputStream = file.getInputStream()) {
                DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                Document document = documentBuilder.parse(inputStream);
                document.getDocumentElement().normalize();
                // 获取根节点
                Element rootNode = document.getDocumentElement();

                // 解析根节点下的子节点
                NodeList firstLevelNodes = rootNode.getChildNodes();
                for (int i = 0; i < firstLevelNodes.getLength(); i++) {
                    Node firstLevelNode = firstLevelNodes.item(i);

                    if (firstLevelNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element firstLevelElement = (Element) firstLevelNode;
                        XmlNodeDTO xmlNodeDTO = new XmlNodeDTO();
                        String nodeName = firstLevelNode.getNodeName();
                        xmlNodeDTO.setNodeName(nodeName);
                        String dataType = new String();
                        String dataLength = new String();
                        String valueName = new String();
                        List<DataContentDTO> dataContent = new ArrayList<>();

                        // 获取第一级子节点的子节点列表
                        NodeList secondLevelNodes = firstLevelElement.getChildNodes();
                        for (int j = 0; j < secondLevelNodes.getLength(); j++) {
                            Node secondLevelNode = secondLevelNodes.item(j);
                            if (secondLevelNode.getNodeType() == Node.ELEMENT_NODE) {
                                Element secondLevelElement = (Element) secondLevelNode;
                                String tagName = secondLevelElement.getTagName();
                                String textContent = secondLevelElement.getTextContent();
                                if (tagName.equalsIgnoreCase("类型")) {
                                    dataType = textContent;
                                } else if (tagName.equalsIgnoreCase("长度")) {
                                    dataLength = textContent;
                                } else if (tagName.equalsIgnoreCase("变量名")) {
                                    valueName = textContent;
                                } else if (tagName.equalsIgnoreCase("数据内容")) {
                                    DataContentDTO dataContentDTO = new DataContentDTO();
                                    NodeList childNodes = secondLevelNode.getChildNodes();
                                    Element dataContentElement = (Element) secondLevelNode;
                                    log.info(childNodes.toString());
                                    log.info("\n长度：" + childNodes.getLength());
                                    if (childNodes.getLength() > 3) {
                                        dataContentDTO.setRemark(dataContentElement.getElementsByTagName("备注").item(0).getTextContent());
                                        dataContentDTO.setContent(dataContentElement.getElementsByTagName("数据").item(0).getTextContent());
                                    } else {
                                        dataContentDTO.setRemark("NULL");
                                        dataContentDTO.setContent(dataContentElement.getElementsByTagName("数据").item(0).getTextContent());
                                    }
                                    dataContent.add(dataContentDTO);
                                }
                            }
                        }
                        xmlNodeDTO.setDataLength(dataLength);
                        xmlNodeDTO.setDataType(dataType);
                        xmlNodeDTO.setDataContent(dataContent);
                        if (StringUtils.isNotEmpty(valueName)) {
                            List<String> valueNames = new ArrayList<>();
                            for (int j = 0; j < Integer.parseInt(dataLength); j++) {
                                if (!dataLength.equalsIgnoreCase("1")) {
                                    valueNames.add(valueName + (j + 1));
                                } else {
                                    valueNames.add(valueName);
                                }
                            }
                            xmlNodeDTO.setValueNames(valueNames);
                        }
                        xmlNodeDTOList.add(xmlNodeDTO);
                    }
                }

                caseList = generateTestCaseList(xmlNodeDTOList, frameList);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        model.addAttribute("frameList", frameList);
        model.addAttribute("caseList", caseList);
        model.addAttribute("xmlNodeDTOList", xmlNodeDTOList);
        return "frame-list";
    }

    private List<TestCase> generateTestCaseList(List<XmlNodeDTO> xmlNodeDTOList, List<DataFrame> frameList) {
        List<TestCase> resCaseList = new ArrayList<>();
        Map<String, List<DataContentDTO>> data = xmlNodeDTOList.stream()
                .collect(Collectors.toMap(XmlNodeDTO::getNodeName, XmlNodeDTO::getDataContent));
        LinkedHashMap<List<String>, List<DataContentDTO>> valueDataMap = xmlNodeDTOList.stream()
                .filter(node -> node.getValueNames() != null)
                .collect(Collectors.toMap(XmlNodeDTO::getValueNames, XmlNodeDTO::getDataContent, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
        Integer caseId = 0;
        Integer abnormalIndex = 0;
        Integer normalIndex = 0;
        // bad case
        for (int i = 0; i < xmlNodeDTOList.size(); i++) {
            XmlNodeDTO dto = xmlNodeDTOList.get(i);
            if (dto.getDataType().equalsIgnoreCase("固定值")) {
                TestCase badCase = new TestCase();
                int fixedValue = Integer.parseInt(data.get(dto.getNodeName()).get(0).getContent().substring(2), 16);
                String errorValue = "0X" + Integer.toHexString(fixedValue + 1).toUpperCase();
                LinkedHashMap<String, String> valueList = new LinkedHashMap<>();
                Boolean hasValueName = true;

                badCase.setId(++caseId);
                badCase.setCaseName("异常-" + dto.getNodeName() + "错误");
                badCase.setCaseSign("testCase" + "-abnormal-" + ++abnormalIndex);
                badCase.setCaseDescription("异常：" + dto.getNodeName() + "错误，" + "错误值为" + errorValue);
                badCase.setFileName("tb_zk_ctrl_testcase_abnormal_" + abnormalIndex);
                badCase.setFilePath("D:\\project\\uartTest\\testcase\\" + "tb_zk_ctrl_testcase_abnormal_" + abnormalIndex + ".sv");
                badCase.setIsRightCase(false);

//                dataFrame.setId(caseId);
//                dataFrame.setIsRightCase(false);
//                for (String frameName : data.keySet()) {
//                    if (fieldMappings.containsKey(frameName)) {
//                        String fieldName = fieldMappings.get(frameName);
//                        String fieldValue = frameName.equalsIgnoreCase(dto.getNodeName())
//                                ? errorValue
//                                : data.get(frameName).get(0).getContent();
//                        setFieldValue(dataFrame, fieldName, fieldValue);
//                    }
//                }
                // 变量名-变量值
                if (ObjectUtils.isEmpty(dto.getValueNames())) {
                    hasValueName = false;
                }
                for (List<String> valueNames : valueDataMap.keySet()) {
                    if (hasValueName) {
                        String values = valueNames.equals(dto.getValueNames())
                                ? errorValue.substring(2)
                                : valueDataMap.get(valueNames).get(0).getContent().substring(2);
                        int chunkSize = values.length() / valueNames.size();
                        int startIndex = 0;
                        int endIndex = chunkSize;

                        for (int j = 0; j < valueNames.size(); j++) {
                            String value = values.substring(startIndex, endIndex);
                            startIndex = endIndex;
                            endIndex = Math.min(values.length(), endIndex + chunkSize);
                            valueList.put(valueNames.get(j), value);
                        }
                    }
                }
                badCase.setValueList(valueList);

                resCaseList.add(badCase);
            } else if (dto.getDataType().equalsIgnoreCase("枚举值")) {
                TestCase badCase = new TestCase();
                List<DataContentDTO> dataContentDTOS = dto.getDataContent();
                List<String> contentList = dataContentDTOS.stream()
                        .map(DataContentDTO::getContent)
                        .collect(Collectors.toList());
                String errorValue = getRandomHex(contentList);
                LinkedHashMap<String, String> valueList = new LinkedHashMap<>();
                Boolean hasValueName = true;

                badCase.setId(++caseId);
                badCase.setCaseName("异常-" + dto.getNodeName() + "错误");
                badCase.setCaseSign("testCase" + "-abnormal-" + ++abnormalIndex);
                badCase.setCaseDescription("异常：" + dto.getNodeName() + "错误，无法识别数据，" + "错误值为" + errorValue);
                badCase.setFileName("tb_zk_ctrl_testcase_abnormal_" + abnormalIndex);
                badCase.setFilePath("D:\\project\\uartTest\\testcase\\" + "tb_zk_ctrl_testcase_abnormal_" + abnormalIndex + ".sv");
                badCase.setIsRightCase(false);

//                dataFrame.setId(caseId);
//                dataFrame.setIsRightCase(false);
//                for (String frameName : data.keySet()) {
//                    if (fieldMappings.containsKey(frameName)) {
//                        String fieldName = fieldMappings.get(frameName);
//                        String fieldValue = frameName.equalsIgnoreCase(dto.getNodeName())
//                                ? errorValue
//                                : data.get(frameName).get(0).getContent();
//                        setFieldValue(dataFrame, fieldName, fieldValue);
//                    }
//                }

                // 变量名-变量值
                if (ObjectUtils.isEmpty(dto.getValueNames())) {
                    hasValueName = false;
                }
                for (List<String> valueNames : valueDataMap.keySet()) {
                    if (hasValueName) {
                        String values = valueNames.equals(dto.getValueNames())
                                ? errorValue.substring(2)
                                : valueDataMap.get(valueNames).get(0).getContent().substring(2);
                        int chunkSize = values.length() / valueNames.size();
                        int startIndex = 0;
                        int endIndex = chunkSize;

                        for (int j = 0; j < valueNames.size(); j++) {
                            String value = values.substring(startIndex, endIndex);
                            startIndex = endIndex;
                            endIndex = Math.min(values.length(), endIndex + chunkSize);
                            valueList.put(valueNames.get(j), value);
                        }
                    }
                }
                badCase.setValueList(valueList);

                resCaseList.add(badCase);
            } else if (dto.getDataType().equalsIgnoreCase("范围值")) {
                TestCase badCaseMin = new TestCase();
                TestCase badCaseMax = new TestCase();
                String errorValueMin = new String();
                String errorValueMax = new String();
                LinkedHashMap<String, String> valueListMin = new LinkedHashMap<>();
                LinkedHashMap<String, String> valueListMax = new LinkedHashMap<>();
                Boolean hasValueName = true;

                List<DataContentDTO> contentDTOS = dto.getDataContent();
                for (DataContentDTO contentDTO : contentDTOS) {
                    if (contentDTO.getRemark().equalsIgnoreCase("最小值")) {
                        int value = Integer.parseInt(contentDTO.getContent().substring(2), 16);
                        errorValueMin = "0X" + Integer.toHexString(value - 1).toUpperCase();
                    } else if (contentDTO.getRemark().equalsIgnoreCase("最大值")) {
                        int value = Integer.parseInt(contentDTO.getContent().substring(2), 16);
                        errorValueMax = "0X" + Integer.toHexString(value + 1).toUpperCase();
                    }
                }
                badCaseMin.setId(++caseId);
                badCaseMin.setCaseName("异常-" + dto.getNodeName() + "错误");
                badCaseMin.setCaseSign("testCase" + "-abnormal-" + ++abnormalIndex);
                badCaseMin.setCaseDescription("异常：" + dto.getNodeName() + "错误，数据小于最小值，" + "错误值为" + errorValueMin);
                badCaseMin.setFileName("tb_zk_ctrl_testcase_abnormal_" + abnormalIndex);
                badCaseMin.setFilePath("D:\\project\\uartTest\\testcase\\" + "tb_zk_ctrl_testcase_abnormal_" + abnormalIndex + ".sv");
                badCaseMin.setIsRightCase(false);

                badCaseMax.setId(++caseId);
                badCaseMax.setCaseName("异常-" + dto.getNodeName() + "错误");
                badCaseMax.setCaseSign("testCase" + "-abnormal-" + ++abnormalIndex);
                badCaseMax.setCaseDescription("异常：" + dto.getNodeName() + "错误，数据大于最大值，" + "错误值为" + errorValueMax);
                badCaseMax.setFileName("tb_zk_ctrl_testcase_abnormal_" + abnormalIndex);
                badCaseMax.setFilePath("D:\\project\\uartTest\\testcase\\" + "tb_zk_ctrl_testcase_abnormal_" + abnormalIndex + ".sv");
                badCaseMax.setIsRightCase(false);

//                for (String frameName : data.keySet()) {
//                    if (fieldMappings.containsKey(frameName)) {
//                        String fieldName = fieldMappings.get(frameName);
//                        String fieldValue = frameName.equalsIgnoreCase(dto.getNodeName())
//                                ? errorValueMin
//                                : data.get(frameName).get(0).getContent();
//                        setFieldValue(dataFrameMin, fieldName, fieldValue);
//                    }
//                }
//                for (String frameName : data.keySet()) {
//                    if (fieldMappings.containsKey(frameName)) {
//                        String fieldName = fieldMappings.get(frameName);
//                        String fieldValue = frameName.equalsIgnoreCase(dto.getNodeName())
//                                ? errorValueMax
//                                : data.get(frameName).get(0).getContent();
//                        setFieldValue(dataFrameMax, fieldName, fieldValue);
//                    }
//                }
                // 变量名-变量值
                if (ObjectUtils.isEmpty(dto.getValueNames())) {
                    hasValueName = false;
                }
                for (List<String> valueNames : valueDataMap.keySet()) {
                    if (hasValueName) {
                        String values = valueNames.equals(dto.getValueNames())
                                ? errorValueMin.substring(2)
                                : valueDataMap.get(valueNames).get(0).getContent().substring(2);
                        int chunkSize = values.length() / valueNames.size();
                        int startIndex = 0;
                        int endIndex = chunkSize;

                        for (int j = 0; j < valueNames.size(); j++) {
                            String value = values.substring(startIndex, endIndex);
                            startIndex = endIndex;
                            endIndex = Math.min(values.length(), endIndex + chunkSize);
                            valueListMin.put(valueNames.get(j), value);
                        }
                    }
                }
                for (List<String> valueNames : valueDataMap.keySet()) {
                    if (hasValueName) {
                        String values = valueNames.equals(dto.getValueNames())
                                ? errorValueMax.substring(2)
                                : valueDataMap.get(valueNames).get(0).getContent().substring(2);
                        int chunkSize = values.length() / valueNames.size();
                        int startIndex = 0;
                        int endIndex = chunkSize;

                        for (int j = 0; j < valueNames.size(); j++) {
                            String value = values.substring(startIndex, endIndex);
                            startIndex = endIndex;
                            endIndex = Math.min(values.length(), endIndex + chunkSize);
                            valueListMax.put(valueNames.get(j), value);
                        }
                    }
                }
                badCaseMin.setValueList(valueListMin);
                badCaseMax.setValueList(valueListMax);


                resCaseList.add(badCaseMin);
                resCaseList.add(badCaseMax);
            }
        }
        // good case
        for (int i = 0; i < xmlNodeDTOList.size(); i++) {
            XmlNodeDTO dto = xmlNodeDTOList.get(i);
            if (dto.getDataType().equalsIgnoreCase("枚举值")) {
                List<DataContentDTO> dataContentDTOS = dto.getDataContent();
                for (DataContentDTO dataContentDTO : dataContentDTOS) {
                    TestCase goodCase = new TestCase();
                    DataFrame dataFrame = new DataFrame();
                    LinkedHashMap<String, String> valueList = new LinkedHashMap<>();
                    Boolean hasValueName = true;

                    goodCase.setId(++caseId);
                    dataFrame.setId(caseId);
                    goodCase.setCaseName("正常-" + dataContentDTO.getRemark());
                    goodCase.setCaseSign("testCase" + "-normal-" + ++abnormalIndex);
                    goodCase.setCaseDescription("正常：" + dataContentDTO.getRemark());
                    goodCase.setFileName("tb_zk_ctrl_testcase_normal_" + ++normalIndex);
                    goodCase.setFilePath("D:\\project\\uartTest\\testcase\\" + "tb_zk_ctrl_testcase_normal_" + normalIndex + ".sv");
                    goodCase.setIsRightCase(true);
                    dataFrame.setIsRightCase(true);

                    for (String frameName : data.keySet()) {
                        if (fieldMappings.containsKey(frameName)) {
                            String fieldName = fieldMappings.get(frameName);
                            String fieldValue = frameName.equalsIgnoreCase(dto.getNodeName())
                                    ? dataContentDTO.getContent()
                                    : data.get(frameName).get(0).getContent();
                            setFieldValue(dataFrame, fieldName, fieldValue);
                        }
                    }

                    // 变量名-变量值
                    if (ObjectUtils.isEmpty(dto.getValueNames())) {
                        hasValueName = false;
                    }
                    for (List<String> valueNames : valueDataMap.keySet()) {
                        if (hasValueName) {
                            String values = valueNames.equals(dto.getValueNames())
                                    ? dataContentDTO.getContent().substring(2)
                                    : valueDataMap.get(valueNames).get(0).getContent().substring(2);
                            int chunkSize = values.length() / valueNames.size();
                            int startIndex = 0;
                            int endIndex = chunkSize;

                            for (int j = 0; j < valueNames.size(); j++) {
                                String value = values.substring(startIndex, endIndex);
                                startIndex = endIndex;
                                endIndex = Math.min(values.length(), endIndex + chunkSize);
                                valueList.put(valueNames.get(j), value);
                            }
                        }
                    }
                    goodCase.setValueList(valueList);

                    frameList.add(dataFrame);
                    resCaseList.add(goodCase);
                }
            }
        }

        return resCaseList;
    }

    @PostMapping("/exportExcel")
    public void exportExcel(@RequestBody List<TestCase> caseList, HttpServletResponse response) throws Exception {
        ExcelUtils.exportExcelToTarget(response, null, caseList, TestCaseExcel.class);
    }

    @PostMapping("/exportSVfile")
    public void exportSVfile(@RequestBody ExportSVDTO exportSVDTO, HttpServletResponse response) throws Exception {
        List<TestCase> caseList = exportSVDTO.getCaseList();
//        List<DataFrame> frameList = exportSVDTO.getFrameList();
        TestCase testCase = caseList.stream()
                .filter(c -> c.getId() == exportSVDTO.getCaseId()) // 筛选id为caseId的TestCase对象
                .findFirst().get();

        String testCaseName = testCase.getCaseName();
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(testCaseName);
//        Integer frameIndex = 0;
//        if (matcher.find()) {
//            String number = matcher.group();
//            frameIndex = Integer.parseInt(number) - 1;
//        }
        String filePath = generateTestCaseData(testCase);
        // 创建资源对象
        Resource resource = loadFileAsResource(filePath);

        if (resource.exists()) {
            // 设置响应头
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + testCaseName + ".sv" + "\"");

            // 将文件内容复制到响应输出流
            FileCopyUtils.copy(resource.getInputStream(), response.getOutputStream());
            response.flushBuffer();
        } else {
            // 文件不存在，返回错误信息
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found: " + testCaseName + ".sv");
        }
    }

    private static String generateTestCaseData(TestCase testCase) throws IOException {
//        // 文件件路径
//        String filePath = "D:\\workspace\\01code\\templeWork\\ToolXML\\ToolXML\\src\\main\\resources\\template\\tb_zk_ctrl_single.sv";
        // 变量-变量值
        LinkedHashMap<String, String> valueList = testCase.getValueList();
        // 获取资源文件路径
        String resourcePath = "template/tb_zk_ctrl_single.sv";

        // 加载资源文件
        ClassLoader classLoader = DataFrameController.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new FileNotFoundException("Resource not found: " + resourcePath);
        }
        // 定义输出文件路径
        String fileName = testCase.getFileName();
        String outputPath = "D:\\project\\uartTest\\testcase\\" + fileName.toLowerCase(Locale.ROOT) + ".sv";
        // 创建文件对象
        File file = new File(outputPath);
        // 获取父目录
        File parentDir = file.getParentFile();
        // 创建父目录及其所有缺失的目录
        if (!parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (created) {
                log.info(fileName + "父目录已成功创建。");
            } else {
                log.info(fileName + "父目录创建失败。");
            }
        }

        // 创建新文件或覆盖已存在的文件
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            StringBuilder contentBuilder = new StringBuilder();
            String line;

            // 逐行读取文件内容
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append("\n");
            }
            String content = contentBuilder.toString();

            // 定义要替换的内容
            String structValNameList = new String();
            String frameContentList= new String();
            String cmdSendBody = new String();

            for (String valueName : valueList.keySet()) {
                structValNameList += "bit [7:0] " + valueName + ";\n";
                frameContentList += "8'h" + valueList.get(valueName) + ", ";
                cmdSendBody += "fr1." + valueName + ", ";
            }

            // 替换大括号中的内容
            content = content.replace("{structValNameList}", structValNameList.substring(0, structValNameList.length() - 1));
            content = content.replace("{frameContentList}", frameContentList.substring(0, frameContentList.length() - 2));
            content = content.replace("{cmdSendBody}", cmdSendBody.substring(0, cmdSendBody.length() - 2));
            content = content.replace("{testCaseName}", testCase.getFileName());

            // 将替换后的内容写入输出文件
            writer.write(content);

            log.info(fileName + "文件已成功创建或覆盖。");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outputPath;
    }

    private Resource loadFileAsResource(String filePath) throws MalformedURLException {
        Path file = Paths.get(filePath).normalize();
        Resource resource = new UrlResource(file.toUri());

        if (resource.exists()) {
            return resource;
        } else {
            throw new RuntimeException("File not found: " + filePath);
        }
    }

    private static String getRandomHex(List<String> list) {
        Random random = new Random();
        String randomHex = "";

        while (true) {
            randomHex = "0x" + Integer.toHexString(random.nextInt(256)).toUpperCase();
            if (!list.contains(randomHex)) {
                break;
            }
        }

        return randomHex;
    }

    private static void setFieldValue(DataFrame dataFrame, String fieldName, String fieldValue) {
        try {
            dataFrame.getClass().getMethod("set" + capitalize(fieldName), String.class).invoke(dataFrame, fieldValue);
        } catch (Exception e) {
            e.printStackTrace();
            // 处理异常
        }
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

}

/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.toolxml.demos.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author <a href="mailto:chenxilzx1@gmail.com">theonefx</a>
 */
@Controller
public class BasicController {

    // http://127.0.0.1:8080/hello?name=lisi
    @RequestMapping("/hello")
    @ResponseBody
    public String hello(@RequestParam(name = "name", defaultValue = "unknown user") String name) {
        return "Hello " + name;
    }

    // http://127.0.0.1:8080/user
    @RequestMapping("/user")
    @ResponseBody
    public User user() {
        User user = new User();
        user.setName("theonefx");
        user.setAge(666);
        return user;
    }

    // http://127.0.0.1:8080/save_user?name=newName&age=11
    @RequestMapping("/save_user")
    @ResponseBody
    public String saveUser(User u) {
        return "user will save: name=" + u.getName() + ", age=" + u.getAge();
    }

    // http://127.0.0.1:8080/html
    @RequestMapping("/html")
    public String html(){
        return "index.html";
    }

    @ModelAttribute
    public void parseUser(@RequestParam(name = "name", defaultValue = "unknown user") String name
            , @RequestParam(name = "age", defaultValue = "12") Integer age, User user) {
        user.setName("zhangsan");
        user.setAge(18);
    }

    /**
     * 下载模板
     * @return
     */
    // http://127.0.0.1:8080/downLoad
    @RequestMapping("/downLoad")
    @ResponseBody
    public void downLoadTemplate(HttpServletResponse response) throws IOException {

        // 创建资源对象
//        Resource resource = loadFileAsResource("D:\\workspace\\01code\\templeWork\\ToolXML\\ToolXML\\src\\main\\resources\\template\\template-V1.1.xml");
        // 获取resources目录下的文件
        Resource resource = new ClassPathResource("template/template-V1.1.xml");
        if (resource.exists()) {
            // 设置响应头
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "template.xml" + "\"");

            // 将文件内容复制到响应输出流
            FileCopyUtils.copy(resource.getInputStream(), response.getOutputStream());
            response.flushBuffer();
        } else {
            // 文件不存在，返回错误信息
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found: " + "template.xml");
        }
    }

//    @PostMapping("/uploadFile")
//    public String uploadFile(@RequestParam("file") MultipartFile file, Model model) {
//        List<DataFrame> frameList = new ArrayList<>();
//
//        if (!file.isEmpty()) {
//            try (InputStream inputStream = file.getInputStream()) {
//                DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
//                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
//                Document document = documentBuilder.parse(inputStream);
//
//                document.getDocumentElement().normalize();
//                NodeList nodeList = document.getElementsByTagName("Frame");
//
//                for (int i = 0; i < nodeList.getLength(); i++) {
//                    Node node = nodeList.item(i);
//
//                    if (node.getNodeType() == Node.ELEMENT_NODE) {
//                        Element element = (Element) node;
//                        String frameHeader = element.getElementsByTagName("FrameHeader").item(0).getTextContent();
//                        String dataLength = element.getElementsByTagName("DataLength").item(0).getTextContent();
//                        String instructionType = element.getElementsByTagName("InstructionType").item(0).getTextContent();
//                        String instructionContent = element.getElementsByTagName("InstructionContent").item(0).getTextContent();
//                        String fixedValue1 = element.getElementsByTagName("FixedValue1").item(0).getTextContent();
//                        String fixedValue2 = element.getElementsByTagName("FixedValue2").item(0).getTextContent();
//                        String fixedValue3 = element.getElementsByTagName("FixedValue3").item(0).getTextContent();
//                        String fixedValue4 = element.getElementsByTagName("FixedValue4").item(0).getTextContent();
//                        String fixedValue5 = element.getElementsByTagName("FixedValue5").item(0).getTextContent();
//                        String fixedValue6 = element.getElementsByTagName("FixedValue6").item(0).getTextContent();
//                        String checksum = element.getElementsByTagName("Checksum").item(0).getTextContent();
//
//                        DataFrame dataFrame = new DataFrame(i + 1, frameHeader, dataLength, instructionType, instructionContent,
//                                fixedValue1, fixedValue2, fixedValue3, fixedValue4, fixedValue5, fixedValue6, checksum);
//                        frameList.add(dataFrame);
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        model.addAttribute("frames", frameList);
//        return "index.html";
//    }


    private Resource loadFileAsResource(String filePath) throws MalformedURLException {
        Path file = Paths.get(filePath).normalize();
        Resource resource = new UrlResource(file.toUri());

        if (resource.exists()) {
            return resource;
        } else {
            throw new RuntimeException("File not found: " + filePath);
        }
    }
}

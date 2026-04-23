package com.wenxi.nekoimagesearchmcpserver.tools;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ImageSearchToolTest {

    @Resource
    private ImageSearchTool imageSearchTool;

    @Test
    void searchImage(){
        String query = "cat";
        String answer = imageSearchTool.searchImage(query);
        Assertions.assertNotNull(answer);
    }

}

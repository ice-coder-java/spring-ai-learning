package com.demo.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
public class ToolsService {

    @Tool(description = "退票")
    public String cancel(@ToolParam(description = "用户姓名") String name,
                         @ToolParam(description = "用户订单号")String orderId) {
        return "退票成功";
    }

}

package com.xcstring.editor.controller;

import com.xcstring.editor.service.XCStringsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/backend/index.php")
@RequiredArgsConstructor
public class XCStringsController {

    private final XCStringsService xcStringsService;

    @PostMapping("/parse")
    public Map<String, Object> parse(@RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        if (content == null || content.isEmpty()) {
            throw new RuntimeException("content is required");
        }
        
        Map<String, Object> parsed = xcStringsService.parse(content);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", parsed);
        return result;
    }

    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestBody Map<String, Object> body) {
        Object data = body.get("data");
        if (data == null) {
            throw new RuntimeException("data is required");
        }
        
        String xcstring = xcStringsService.generate(data);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("xcstring", xcstring);
        return result;
    }
}

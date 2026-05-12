package com.xcstring.editor.controller;

import com.xcstring.editor.service.FileManagerService;
import com.xcstring.editor.service.XCStringsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/backend/index.php")
@RequiredArgsConstructor
public class UploadController {

    private final FileManagerService fileManagerService;
    private final XCStringsService xcStringsService;

    @GetMapping("/upload/{token}")
    public Map<String, Object> getUploadInstructions(@PathVariable String token) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "Ready for upload");
        result.put("instructions", "Send PUT request with xcstrings content in body");
        return result;
    }

    @PutMapping("/upload/{token}")
    public Map<String, Object> uploadViaToken(
            @PathVariable String token,
            @RequestBody String content,
            @RequestHeader(value = "X-Comment", required = false) String comment) {
        
        if (content == null || content.isEmpty()) {
            throw new RuntimeException("Content is required");
        }
        
        xcStringsService.parse(content);
        
        Map<String, Object> uploadResult = fileManagerService.uploadViaPresignedUrl(token, content, comment);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", uploadResult);
        return result;
    }
}

package com.xcstring.editor.controller;

import com.xcstring.editor.entity.User;
import com.xcstring.editor.security.SessionAuthenticationFilter;
import com.xcstring.editor.service.AIService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @PostMapping("/ai/batch-translate")
    public Map<String, Object> batchTranslate(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User user = SessionAuthenticationFilter.getCurrentUser(request);
        
        Object itemsObj = body.get("items");
        String sourceLanguage = (String) body.get("source_language");
        String targetLanguage = (String) body.get("target_language");
        
        if (itemsObj == null) {
            throw new RuntimeException("items is required");
        }
        if (!(itemsObj instanceof List)) {
            throw new RuntimeException("items must be a list");
        }
        if (sourceLanguage == null || sourceLanguage.isEmpty()) {
            throw new RuntimeException("source_language is required");
        }
        if (targetLanguage == null || targetLanguage.isEmpty()) {
            throw new RuntimeException("target_language is required");
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) itemsObj;
        
        String provider = (String) body.get("provider");
        String model = (String) body.get("model");
        
        List<Map<String, Object>> translations = aiService.batchTranslate(
            items,
            sourceLanguage,
            targetLanguage,
            provider,
            model
        );
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("translations", translations);
        return result;
    }

    @PostMapping("/ai/batch-proofread")
    public Map<String, Object> batchProofread(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User user = SessionAuthenticationFilter.getCurrentUser(request);
        
        Object itemsObj = body.get("items");
        String language = (String) body.get("language");
        
        if (itemsObj == null) {
            throw new RuntimeException("items is required");
        }
        if (!(itemsObj instanceof List)) {
            throw new RuntimeException("items must be a list");
        }
        if (language == null || language.isEmpty()) {
            throw new RuntimeException("language is required");
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) itemsObj;
        
        String provider = (String) body.get("provider");
        String model = (String) body.get("model");
        
        List<Map<String, Object>> reviews = aiService.batchProofread(
            items,
            language,
            provider,
            model
        );
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("reviews", reviews);
        return result;
    }
}

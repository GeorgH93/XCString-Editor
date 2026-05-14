package com.xcstring.editor.controller;

import com.xcstring.editor.entity.User;
import com.xcstring.editor.security.SessionAuthenticationFilter;
import com.xcstring.editor.service.FileManagerService;
import com.xcstring.editor.service.XCStringsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileController {

    private final FileManagerService fileManagerService;
    private final XCStringsService xcStringsService;

    @PostMapping("/files/save")
    public Map<String, Object> saveFile(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User user = SessionAuthenticationFilter.getCurrentUser(request);
        
        String name = (String) body.get("name");
        String content = (String) body.get("content");
        if (name == null || name.isEmpty()) {
            throw new RuntimeException("Name is required");
        }
        if (content == null || content.isEmpty()) {
            throw new RuntimeException("Content is required");
        }
        
        boolean isPublic = Boolean.TRUE.equals(body.get("is_public"));
        String comment = (String) body.get("comment");
        
        Long fileId = fileManagerService.saveFile(user.getId(), name, content, isPublic, comment);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("file_id", fileId);
        return result;
    }

    @PostMapping("/files/update")
    public Map<String, Object> updateFile(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User user = SessionAuthenticationFilter.getCurrentUser(request);
        
        Object fileIdObj = body.get("file_id");
        if (fileIdObj == null) {
            throw new RuntimeException("file_id is required");
        }
        Long fileId = ((Number) fileIdObj).longValue();
        
        String content = (String) body.get("content");
        if (content == null || content.isEmpty()) {
            throw new RuntimeException("Content is required");
        }
        
        String comment = (String) body.get("comment");
        
        fileManagerService.updateFile(fileId, user.getId(), content, comment);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        return result;
    }

    @PostMapping("/files/{id}/upload-version")
    public Map<String, Object> uploadVersion(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String comment,
            HttpServletRequest request) throws Exception {
        
        User user = SessionAuthenticationFilter.getCurrentUser(request);
        
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        
        xcStringsService.parse(content);
        
        String finalComment = comment != null ? comment : "Uploaded new version";
        
        fileManagerService.updateFile(id, user.getId(), content, finalComment);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "Version uploaded successfully");
        return result;
    }

    @PostMapping("/files/share")
    public Map<String, Object> shareFile(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User user = SessionAuthenticationFilter.getCurrentUser(request);
        
        Object fileIdObj = body.get("file_id");
        if (fileIdObj == null) {
            throw new RuntimeException("file_id is required");
        }
        Long fileId = ((Number) fileIdObj).longValue();
        
        String email = (String) body.get("email");
        if (email == null || email.isEmpty()) {
            throw new RuntimeException("email is required");
        }
        
        boolean canEdit = Boolean.TRUE.equals(body.get("can_edit"));
        
        fileManagerService.shareFileWithEmail(fileId, user.getId(), email, canEdit);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        return result;
    }

    @PostMapping("/files/update-share-permissions")
    public Map<String, Object> updateSharePermissions(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User user = SessionAuthenticationFilter.getCurrentUser(request);
        
        Object fileIdObj = body.get("file_id");
        if (fileIdObj == null) {
            throw new RuntimeException("file_id is required");
        }
        Long fileId = ((Number) fileIdObj).longValue();
        
        Object shareIdObj = body.get("share_id");
        if (shareIdObj == null) {
            throw new RuntimeException("share_id is required");
        }
        Long shareId = ((Number) shareIdObj).longValue();
        
        boolean canEdit = Boolean.TRUE.equals(body.get("can_edit"));
        
        fileManagerService.updateSharePermissions(fileId, user.getId(), shareId, canEdit);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        return result;
    }

    @PostMapping("/files/{id}/revert")
    public Map<String, Object> revertToVersion(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        
        User user = SessionAuthenticationFilter.getCurrentUser(request);
        
        Object versionNumberObj = body.get("version_number");
        if (versionNumberObj == null) {
            throw new RuntimeException("version_number is required");
        }
        Integer versionNumber = ((Number) versionNumberObj).intValue();
        
        String comment = (String) body.get("comment");
        
        fileManagerService.revertToVersion(id, versionNumber, user.getId(), comment);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        return result;
    }

    @PostMapping("/files/{id}/generate-upload-url")
    public Map<String, Object> generateUploadUrl(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        
        User user = SessionAuthenticationFilter.getCurrentUser(request);
        
        String commentPrefix = body != null ? (String) body.get("comment_prefix") : null;
        
        Map<String, Object> urlData = fileManagerService.generatePresignedUploadUrl(id, user.getId(), commentPrefix);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", urlData);
        return result;
    }

    @GetMapping("/files/my")
    public Map<String, Object> getMyFiles(HttpServletRequest request) {
        User user = SessionAuthenticationFilter.getCurrentUser(request);
        
        List<Map<String, Object>> files = fileManagerService.getUserFiles(user.getId());
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("files", files);
        return result;
    }

    @GetMapping("/files/shared")
    public Map<String, Object> getSharedFiles(HttpServletRequest request) {
        User user = SessionAuthenticationFilter.getCurrentUser(request);
        
        List<Map<String, Object>> files = fileManagerService.getSharedFiles(user.getId());
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("files", files);
        return result;
    }

    @GetMapping("/files/public")
    public Map<String, Object> getPublicFiles() {
        List<Map<String, Object>> files = fileManagerService.getPublicFiles();
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("files", files);
        return result;
    }

    @GetMapping("/files/{id}/versions/{versionNumber}")
    public Map<String, Object> getFileVersion(
            @PathVariable Long id,
            @PathVariable Integer versionNumber,
            HttpServletRequest request) {
        
        Optional<User> userOpt = SessionAuthenticationFilter.getOptionalUser(request);
        Long userId = userOpt.map(User::getId).orElse(null);
        
        Map<String, Object> version = fileManagerService.getFileVersion(id, versionNumber, userId);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("version", version);
        return result;
    }

    @GetMapping("/files/{id}/version-stats")
    public Map<String, Object> getFileVersionStats(
            @PathVariable Long id,
            HttpServletRequest request) {
        
        Optional<User> userOpt = SessionAuthenticationFilter.getOptionalUser(request);
        Long userId = userOpt.map(User::getId).orElse(null);
        
        Map<String, Object> stats = fileManagerService.getFileVersionStats(id, userId);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("stats", stats);
        return result;
    }

    @GetMapping("/files/{id}/versions")
    public Map<String, Object> getFileVersions(
            @PathVariable Long id,
            HttpServletRequest request) {
        
        Optional<User> userOpt = SessionAuthenticationFilter.getOptionalUser(request);
        Long userId = userOpt.map(User::getId).orElse(null);
        
        List<Map<String, Object>> versions = fileManagerService.getFileVersions(id, userId);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("versions", versions);
        return result;
    }

    @GetMapping("/files/{id}/shares")
    public Map<String, Object> getFileShares(@PathVariable Long id, HttpServletRequest request) {
        User user = SessionAuthenticationFilter.getCurrentUser(request);
        
        List<Map<String, Object>> shares = fileManagerService.getFileShares(id, user.getId());
        List<Map<String, Object>> pendingShares = fileManagerService.getPendingShares(id, user.getId());
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("shares", shares);
        result.put("pending_shares", pendingShares);
        return result;
    }

    @GetMapping("/files/{id}/upload-urls")
    public Map<String, Object> getUploadUrls(@PathVariable Long id, HttpServletRequest request) {
        User user = SessionAuthenticationFilter.getCurrentUser(request);
        
        List<Map<String, Object>> urls = fileManagerService.getPresignedUrls(id, user.getId());
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("urls", urls);
        return result;
    }

    @GetMapping("/files/{id}")
    public Map<String, Object> getFile(@PathVariable Long id, HttpServletRequest request) {
        Optional<User> userOpt = SessionAuthenticationFilter.getOptionalUser(request);
        Long userId = userOpt.map(User::getId).orElse(null);
        
        Map<String, Object> file = fileManagerService.getFile(id, userId);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("file", file);
        return result;
    }

    @DeleteMapping("/files/{id}/versions/{versionNumber}")
    public Map<String, Object> deleteFileVersion(
            @PathVariable Long id,
            @PathVariable Integer versionNumber,
            HttpServletRequest request) {
        
        User user = SessionAuthenticationFilter.getCurrentUser(request);
        
        fileManagerService.deleteFileVersion(id, versionNumber, user.getId());
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        return result;
    }

    @DeleteMapping("/files/{id}/pending-shares/{shareId}")
    public Map<String, Object> deletePendingShare(
            @PathVariable Long id,
            @PathVariable Long shareId,
            HttpServletRequest request) {
        
        User user = SessionAuthenticationFilter.getCurrentUser(request);
        
        fileManagerService.removePendingShare(id, user.getId(), shareId);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        return result;
    }

    @DeleteMapping("/files/{id}/shares/{targetUserId}")
    public Map<String, Object> deleteShare(
            @PathVariable Long id,
            @PathVariable Long targetUserId,
            HttpServletRequest request) {
        
        User user = SessionAuthenticationFilter.getCurrentUser(request);
        
        fileManagerService.unshareFile(id, user.getId(), targetUserId);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        return result;
    }

    @DeleteMapping("/files/{id}/upload-urls/{urlId}")
    public Map<String, Object> deleteUploadUrl(
            @PathVariable Long id,
            @PathVariable Long urlId,
            HttpServletRequest request) {
        
        User user = SessionAuthenticationFilter.getCurrentUser(request);
        
        fileManagerService.revokePresignedUrl(id, urlId, user.getId());
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        return result;
    }

    @DeleteMapping("/files/{id}")
    public Map<String, Object> deleteFile(@PathVariable Long id, HttpServletRequest request) {
        User user = SessionAuthenticationFilter.getCurrentUser(request);
        
        fileManagerService.deleteFile(id, user.getId());
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        return result;
    }
}

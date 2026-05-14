package com.xcstring.editor.service;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.xcstring.editor.config.AppProperties;
import com.xcstring.editor.entity.*;
import com.xcstring.editor.repository.*;
import com.xcstring.editor.util.ContentHashUtil;
import com.xcstring.editor.util.SecureTokenGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileManagerService {
    private final XCStringFileRepository fileRepository;
    private final FileVersionRepository versionRepository;
    private final FileShareRepository shareRepository;
    private final PendingShareRepository pendingShareRepository;
    private final UserRepository userRepository;
    private final PresignedUploadUrlRepository presignedUrlRepository;
    private final AppProperties properties;
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ---- CORE FILE OPERATIONS ----

    @Transactional
    public Long saveFile(Long userId, String name, String content, boolean isPublic, String comment) {
        // Validate size
        if (content.getBytes().length > properties.getFiles().getMaxFileSize()) {
            throw new RuntimeException("File size exceeds maximum allowed size");
        }
        // Check user file limit
        long fileCount = fileRepository.countByUserId(userId);
        if (fileCount >= properties.getFiles().getMaxFilesPerUser()) {
            throw new RuntimeException("Maximum number of files reached");
        }
        // Validate JSON
        validateJson(content);
        
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        
        XCStringFile file = new XCStringFile();
        file.setUser(user);
        file.setName(name);
        file.setContent(content);
        file.setIsPublic(isPublic);
        fileRepository.save(file);
        
        // Create initial version
        createFileVersion(file, content, user, comment != null ? comment : "Initial version");
        
        return file.getId();
    }

    @Transactional
    public void updateFile(Long fileId, Long userId, String content, String comment) {
        if (!canEditFile(fileId, userId)) throw new RuntimeException("Permission denied");
        if (content.getBytes().length > properties.getFiles().getMaxFileSize()) {
            throw new RuntimeException("File size exceeds maximum allowed size");
        }
        validateJson(content);
        
        XCStringFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        
        // Only update if content changed (PHP behavior)
        String currentHash = ContentHashUtil.sha256(file.getContent());
        String newHash = ContentHashUtil.sha256(content);
        
        if (!currentHash.equals(newHash)) {
            file.setContent(content);
            fileRepository.save(file);
            
            User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
            createFileVersion(file, content, user, comment);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getFile(Long fileId, Long userId) {
        XCStringFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        
        if (!canViewFile(fileId, userId)) throw new RuntimeException("Permission denied");
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", file.getId());
        result.put("user_id", file.getUser().getId());
        result.put("name", file.getName());
        result.put("content", file.getContent());
        result.put("is_public", file.getIsPublic() != null && file.getIsPublic() ? 1 : 0);
        result.put("created_at", formatDateTime(file.getCreatedAt()));
        result.put("updated_at", formatDateTime(file.getUpdatedAt()));
        result.put("owner_name", file.getUser().getName());
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserFiles(Long userId) {
        List<XCStringFile> files = fileRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (XCStringFile f : files) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", f.getId());
            m.put("name", f.getName());
            m.put("is_public", f.getIsPublic() != null && f.getIsPublic() ? 1 : 0);
            m.put("created_at", formatDateTime(f.getCreatedAt()));
            m.put("updated_at", formatDateTime(f.getUpdatedAt()));
            result.add(m);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSharedFiles(Long userId) {
        List<XCStringFile> files = fileRepository.findSharedWithUser(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (XCStringFile f : files) {
            Optional<FileShare> share = shareRepository.findByFileIdAndSharedWithUserId(f.getId(), userId);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", f.getId());
            m.put("name", f.getName());
            m.put("created_at", formatDateTime(f.getCreatedAt()));
            m.put("updated_at", formatDateTime(f.getUpdatedAt()));
            m.put("owner_name", f.getUser().getName());
            m.put("can_edit", share.map(s -> s.getCanEdit() != null && s.getCanEdit() ? 1 : 0).orElse(0));
            result.add(m);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPublicFiles() {
        List<XCStringFile> files = fileRepository.findByIsPublicTrueOrderByUpdatedAtDesc(PageRequest.of(0, 50));
        List<Map<String, Object>> result = new ArrayList<>();
        for (XCStringFile f : files) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", f.getId());
            m.put("name", f.getName());
            m.put("created_at", formatDateTime(f.getCreatedAt()));
            m.put("updated_at", formatDateTime(f.getUpdatedAt()));
            m.put("owner_name", f.getUser().getName());
            result.add(m);
        }
        return result;
    }

    @Transactional
    public void deleteFile(Long fileId, Long userId) {
        XCStringFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        if (!file.getUser().getId().equals(userId)) throw new RuntimeException("Permission denied");
        fileRepository.delete(file);
    }

    // ---- SHARING ----

    @Transactional
    public void shareFileWithEmail(Long fileId, Long ownerId, String shareWithEmail, boolean canEdit) {
        XCStringFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        if (!file.getUser().getId().equals(ownerId)) throw new RuntimeException("Permission denied");
        
        // Validate email
        if (!shareWithEmail.contains("@")) throw new RuntimeException("Invalid email address");
        
        // Don't share with self
        User owner = userRepository.findById(ownerId).orElseThrow();
        if (owner.getEmail().equals(shareWithEmail)) throw new RuntimeException("Cannot share with yourself");
        
        Optional<User> targetUser = userRepository.findByEmail(shareWithEmail);
        if (targetUser.isPresent()) {
            // User exists, create/update share
            shareFile(fileId, ownerId, shareWithEmail, canEdit);
        } else {
            // Create pending share
            Optional<PendingShare> existing = pendingShareRepository.findByFileIdAndSharedWithEmail(fileId, shareWithEmail);
            if (existing.isPresent()) {
                existing.get().setCanEdit(canEdit);
                pendingShareRepository.save(existing.get());
            } else {
                PendingShare ps = new PendingShare();
                ps.setFile(file);
                ps.setSharedByUser(owner);
                ps.setSharedWithEmail(shareWithEmail);
                ps.setCanEdit(canEdit);
                pendingShareRepository.save(ps);
            }
        }
    }

    private void shareFile(Long fileId, Long ownerId, String shareWithEmail, boolean canEdit) {
        XCStringFile file = fileRepository.findById(fileId).orElseThrow();
        if (!file.getUser().getId().equals(ownerId)) throw new RuntimeException("Permission denied");
        
        User shareWithUser = userRepository.findByEmail(shareWithEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (shareWithUser.getId().equals(ownerId)) throw new RuntimeException("Cannot share with yourself");
        
        Optional<FileShare> existing = shareRepository.findByFileIdAndSharedWithUserId(fileId, shareWithUser.getId());
        if (existing.isPresent()) {
            existing.get().setCanEdit(canEdit);
            shareRepository.save(existing.get());
        } else {
            FileShare share = new FileShare();
            share.setFile(file);
            share.setSharedWithUser(shareWithUser);
            share.setCanEdit(canEdit);
            shareRepository.save(share);
        }
    }

    @Transactional
    public void unshareFile(Long fileId, Long ownerId, Long unshareUserId) {
        XCStringFile file = fileRepository.findById(fileId).orElseThrow(() -> new RuntimeException("File not found"));
        if (!file.getUser().getId().equals(ownerId)) throw new RuntimeException("Permission denied");
        shareRepository.deleteByFileIdAndSharedWithUserId(fileId, unshareUserId);
    }

    @Transactional
    public void updateSharePermissions(Long fileId, Long ownerId, Long shareId, boolean canEdit) {
        XCStringFile file = fileRepository.findById(fileId).orElseThrow(() -> new RuntimeException("File not found"));
        if (!file.getUser().getId().equals(ownerId)) throw new RuntimeException("Permission denied");
        shareRepository.findById(shareId).ifPresent(share -> {
            if (share.getFile().getId().equals(fileId)) {
                share.setCanEdit(canEdit);
                shareRepository.save(share);
            }
        });
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFileShares(Long fileId, Long ownerId) {
        XCStringFile file = fileRepository.findById(fileId).orElseThrow(() -> new RuntimeException("File not found"));
        if (!file.getUser().getId().equals(ownerId)) throw new RuntimeException("Permission denied");
        
        List<FileShare> shares = shareRepository.findByFileIdWithUser(fileId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (FileShare s : shares) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("file_id", fileId);
            m.put("shared_with_user_id", s.getSharedWithUser().getId());
            m.put("can_edit", s.getCanEdit() != null && s.getCanEdit() ? 1 : 0);
            m.put("created_at", formatDateTime(s.getCreatedAt()));
            m.put("email", s.getSharedWithUser().getEmail());
            m.put("name", s.getSharedWithUser().getName());
            result.add(m);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPendingShares(Long fileId, Long ownerId) {
        XCStringFile file = fileRepository.findById(fileId).orElseThrow(() -> new RuntimeException("File not found"));
        if (!file.getUser().getId().equals(ownerId)) throw new RuntimeException("Permission denied");
        
        List<PendingShare> shares = pendingShareRepository.findByFileIdOrderByCreatedAtDesc(fileId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (PendingShare ps : shares) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", ps.getId());
            m.put("shared_with_email", ps.getSharedWithEmail());
            m.put("can_edit", ps.getCanEdit() != null && ps.getCanEdit() ? 1 : 0);
            m.put("created_at", formatDateTime(ps.getCreatedAt()));
            result.add(m);
        }
        return result;
    }

    @Transactional
    public void removePendingShare(Long fileId, Long ownerId, Long shareId) {
        XCStringFile file = fileRepository.findById(fileId).orElseThrow(() -> new RuntimeException("File not found"));
        if (!file.getUser().getId().equals(ownerId)) throw new RuntimeException("Permission denied");
        pendingShareRepository.findById(shareId).ifPresent(ps -> {
            if (ps.getFile().getId().equals(fileId)) pendingShareRepository.delete(ps);
        });
    }

    @Transactional
    public int convertPendingSharesForNewUser(String email, Long userId) {
        List<PendingShare> pending = pendingShareRepository.findBySharedWithEmail(email);
        User user = userRepository.findById(userId).orElseThrow();
        for (PendingShare ps : pending) {
            if (!shareRepository.existsByFileIdAndSharedWithUserId(ps.getFile().getId(), userId)) {
                FileShare share = new FileShare();
                share.setFile(ps.getFile());
                share.setSharedWithUser(user);
                share.setCanEdit(ps.getCanEdit());
                shareRepository.save(share);
            }
        }
        pendingShareRepository.deleteBySharedWithEmail(email);
        return pending.size();
    }

    // ---- VERSIONS ----

    private void createFileVersion(XCStringFile file, String content, User user, String comment) {
        int nextVersion = versionRepository.findMaxVersionByFileId(file.getId())
                .map(v -> v + 1).orElse(1);
        String hash = ContentHashUtil.sha256(content);
        
        // Deduplication: don't create version if content already exists
        if (versionRepository.findByFileIdAndContentHash(file.getId(), hash).isPresent()) return;
        
        FileVersion version = new FileVersion();
        version.setFile(file);
        version.setVersionNumber(nextVersion);
        version.setContent(content);
        version.setComment(comment);
        version.setCreatedByUser(user);
        version.setContentHash(hash);
        version.setSizeBytes((long) content.getBytes().length);
        versionRepository.save(version);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFileVersions(Long fileId, Long userId) {
        if (!canViewFile(fileId, userId)) throw new RuntimeException("Permission denied");
        List<FileVersion> versions = versionRepository.findByFileIdOrderByVersionNumberDesc(fileId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (FileVersion v : versions) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", v.getId());
            m.put("version_number", v.getVersionNumber());
            m.put("comment", v.getComment());
            m.put("created_at", formatDateTime(v.getCreatedAt()));
            m.put("size_bytes", v.getSizeBytes());
            m.put("created_by_name", v.getCreatedByUser().getName());
            m.put("created_by_email", v.getCreatedByUser().getEmail());
            result.add(m);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getFileVersion(Long fileId, Integer versionNumber, Long userId) {
        if (!canViewFile(fileId, userId)) throw new RuntimeException("Permission denied");
        FileVersion v = versionRepository.findByFileIdAndVersionNumber(fileId, versionNumber)
                .orElseThrow(() -> new RuntimeException("Version not found"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", v.getId());
        m.put("file_id", fileId);
        m.put("version_number", v.getVersionNumber());
        m.put("content", v.getContent());
        m.put("comment", v.getComment());
        m.put("created_at", formatDateTime(v.getCreatedAt()));
        m.put("content_hash", v.getContentHash());
        m.put("size_bytes", v.getSizeBytes());
        m.put("created_by_name", v.getCreatedByUser().getName());
        m.put("created_by_email", v.getCreatedByUser().getEmail());
        return m;
    }

    @Transactional
    public void revertToVersion(Long fileId, Integer versionNumber, Long userId, String comment) {
        if (!canEditFile(fileId, userId)) throw new RuntimeException("Permission denied");
        Map<String, Object> version = getFileVersion(fileId, versionNumber, userId);
        String revertComment = comment != null ? comment : "Reverted to version " + versionNumber;
        updateFile(fileId, userId, (String) version.get("content"), revertComment);
    }

    @Transactional
    public void deleteFileVersion(Long fileId, Integer versionNumber, Long userId) {
        XCStringFile file = fileRepository.findById(fileId).orElseThrow(() -> new RuntimeException("File not found"));
        if (!file.getUser().getId().equals(userId)) throw new RuntimeException("Permission denied");
        
        Optional<Integer> maxVersion = versionRepository.findMaxVersionByFileId(fileId);
        if (maxVersion.isPresent() && maxVersion.get().equals(versionNumber)) {
            throw new RuntimeException("Cannot delete the latest version");
        }
        long count = versionRepository.countByFileId(fileId);
        if (count <= 1) throw new RuntimeException("Cannot delete the only version");
        
        versionRepository.findByFileIdAndVersionNumber(fileId, versionNumber)
                .ifPresent(versionRepository::delete);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getFileVersionStats(Long fileId, Long userId) {
        if (!canViewFile(fileId, userId)) throw new RuntimeException("Permission denied");
        
        List<FileVersion> versions = versionRepository.findByFileIdOrderByVersionNumberDesc(fileId);
        if (versions.isEmpty()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("total_versions", 0);
            m.put("first_version_date", null);
            m.put("latest_version_date", null);
            m.put("total_size_bytes", 0L);
            m.put("unique_contributors", 0);
            return m;
        }
        
        long totalSize = versions.stream().mapToLong(FileVersion::getSizeBytes).sum();
        long uniqueContributors = versions.stream().map(v -> v.getCreatedByUser().getId()).distinct().count();
        LocalDateTime firstDate = versions.stream().map(FileVersion::getCreatedAt).min(LocalDateTime::compareTo).orElse(null);
        LocalDateTime latestDate = versions.stream().map(FileVersion::getCreatedAt).max(LocalDateTime::compareTo).orElse(null);
        
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total_versions", versions.size());
        m.put("first_version_date", firstDate != null ? formatDateTime(firstDate) : null);
        m.put("latest_version_date", latestDate != null ? formatDateTime(latestDate) : null);
        m.put("total_size_bytes", totalSize);
        m.put("unique_contributors", uniqueContributors);
        return m;
    }

    // ---- PRESIGNED URLs ----

    @Transactional
    public Map<String, Object> generatePresignedUploadUrl(Long fileId, Long userId, String commentPrefix) {
        if (!canEditFile(fileId, userId)) throw new RuntimeException("Permission denied");
        
        String token = SecureTokenGenerator.generate(32); // 64-char hex
        LocalDateTime expiresAt = LocalDateTime.now().plusYears(1);
        
        XCStringFile file = fileRepository.findById(fileId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        
        PresignedUploadUrl url = new PresignedUploadUrl();
        url.setFile(file);
        url.setToken(token);
        url.setCreatedByUser(user);
        url.setExpiresAt(expiresAt);
        url.setCommentPrefix(commentPrefix);
        presignedUrlRepository.save(url);
        
        String uploadUrl = properties.getApp().getBaseUrl() + "/api/upload/" + token;
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("expires_at", formatDateTime(expiresAt));
        result.put("upload_url", uploadUrl);
        return result;
    }

    @Transactional
    public Map<String, Object> uploadViaPresignedUrl(String token, String content, String comment) {
        PresignedUploadUrl presignedUrl = presignedUrlRepository
                .findValidByToken(token, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Invalid or expired upload token"));
        
        // Build comment
        String finalComment = comment;
        if (presignedUrl.getCommentPrefix() != null) {
            finalComment = presignedUrl.getCommentPrefix() + (comment != null ? " - " + comment : "");
        }
        if (finalComment == null || finalComment.isBlank()) {
            finalComment = "Uploaded via presigned URL";
        }
        
        updateFile(presignedUrl.getFile().getId(), presignedUrl.getCreatedByUser().getId(), content, finalComment);
        
        // Update used_at but keep URL reusable
        presignedUrl.setUsedAt(LocalDateTime.now());
        presignedUrlRepository.save(presignedUrl);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("file_id", presignedUrl.getFile().getId());
        result.put("message", "Version uploaded successfully via presigned URL");
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPresignedUrls(Long fileId, Long userId) {
        if (!canViewFile(fileId, userId)) throw new RuntimeException("Permission denied");
        List<PresignedUploadUrl> urls = presignedUrlRepository.findByFileIdOrderByCreatedAtDesc(fileId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (PresignedUploadUrl u : urls) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("token", u.getToken());
            m.put("created_at", formatDateTime(u.getCreatedAt()));
            m.put("expires_at", formatDateTime(u.getExpiresAt()));
            m.put("used_at", u.getUsedAt() != null ? formatDateTime(u.getUsedAt()) : null);
            m.put("comment_prefix", u.getCommentPrefix());
            result.add(m);
        }
        return result;
    }

    @Transactional
    public void revokePresignedUrl(Long fileId, Long urlId, Long userId) {
        if (!canEditFile(fileId, userId)) throw new RuntimeException("Permission denied");
        presignedUrlRepository.findById(urlId).ifPresent(u -> {
            if (u.getFile().getId().equals(fileId)) presignedUrlRepository.delete(u);
        });
    }

    // ---- PERMISSIONS ----

    private boolean canViewFile(Long fileId, Long userId) {
        Optional<XCStringFile> file = fileRepository.findById(fileId);
        if (file.isEmpty()) return false;
        XCStringFile f = file.get();
        if (userId != null && f.getUser().getId().equals(userId)) return true;
        if (f.getIsPublic() != null && f.getIsPublic()) return true;
        if (userId != null) return shareRepository.existsByFileIdAndSharedWithUserId(fileId, userId);
        return false;
    }

    private boolean canEditFile(Long fileId, Long userId) {
        Optional<XCStringFile> file = fileRepository.findById(fileId);
        if (file.isEmpty()) return false;
        if (file.get().getUser().getId().equals(userId)) return true;
        Optional<FileShare> share = shareRepository.findByFileIdAndSharedWithUserId(fileId, userId);
        return share.isPresent() && share.get().getCanEdit() != null && share.get().getCanEdit();
    }

    // ---- HELPERS ----

    private void validateJson(String content) {
        try {
            JsonParser.parseString(content);
        } catch (JsonSyntaxException e) {
            throw new RuntimeException("Invalid xcstring content: " + e.getMessage());
        }
    }

    private String formatDateTime(LocalDateTime dt) {
        if (dt == null) return null;
        return dt.format(DATE_FORMAT);
    }
}

package com.xcstring.editor.repository;

import com.xcstring.editor.entity.OAuth2Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OAuth2AccountRepository extends JpaRepository<OAuth2Account, Long> {
    Optional<OAuth2Account> findByProviderAndProviderUserId(String provider, String providerUserId);
    List<OAuth2Account> findByUserId(Long userId);
    List<OAuth2Account> findByUserIdAndProviderNot(Long userId, String provider);
    void deleteByUserIdAndProvider(Long userId, String provider);
    boolean existsByUserIdAndProvider(Long userId, String provider);
}

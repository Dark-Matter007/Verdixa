package com.leetcode.backend.repository;
import com.leetcode.backend.model.TranslationCache; import org.springframework.data.jpa.repository.JpaRepository; import java.util.Optional;
public interface TranslationCacheRepository extends JpaRepository<TranslationCache,Long>{Optional<TranslationCache> findBySourceLanguageAndTargetLanguageAndSourceHash(String source,String target,String hash);}

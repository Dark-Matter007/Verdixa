package com.leetcode.backend.config;
import com.leetcode.backend.repository.UserRepository;
import com.leetcode.backend.service.CertificateService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
@Configuration
public class CertificateBackfill {
    @Bean @Order(130) CommandLineRunner backfillCertificates(UserRepository users,CertificateService certificates){
        return args->{int page=0;org.springframework.data.domain.Page<com.leetcode.backend.model.User> batch;
            do {batch=users.findAll(PageRequest.of(page++,200));for(var user:batch)certificates.evaluate(user.getId());}while(batch.hasNext());
        };
    }
}

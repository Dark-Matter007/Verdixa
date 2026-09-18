package com.leetcode.backend.repository;
import com.leetcode.backend.model.*; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery,Long>{boolean existsByUserIdAndTypeAndEntityTypeAndEntityId(Long userId,String type,String entityType,Long entityId); List<NotificationDelivery> findTop100ByStatusOrderByIdAsc(AssessmentEnums.DeliveryStatus status);}

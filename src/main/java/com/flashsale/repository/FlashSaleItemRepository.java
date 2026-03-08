package com.flashsale.repository;

import com.flashsale.entity.FlashSaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FlashSaleItemRepository extends JpaRepository<FlashSaleItem, Long> {

    @Query("SELECT f FROM FlashSaleItem f JOIN FETCH f.product WHERE f.startTime <= :now AND f.endTime >= :now")
    List<FlashSaleItem> findActiveFlashSaleItems(LocalDateTime now);
}

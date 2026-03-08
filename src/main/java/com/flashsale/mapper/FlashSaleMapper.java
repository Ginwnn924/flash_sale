package com.flashsale.mapper;

import com.flashsale.dto.FlashSaleProductDTO;
import com.flashsale.dto.OrderResponseDTO;
import com.flashsale.entity.FlashSaleItem;
import com.flashsale.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FlashSaleMapper {

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.brand", target = "brand")
    @Mapping(source = "product.model", target = "model")
    @Mapping(source = "product.storage", target = "storage")
    @Mapping(source = "product.color", target = "color")
    @Mapping(source = "flashPrice", target = "flashPrice")
    @Mapping(source = "totalStock", target = "remainingStock")
    FlashSaleProductDTO toFlashSaleProductDTO(FlashSaleItem entity);

    @Mapping(source = "id", target = "orderId")
    @Mapping(target = "status", expression = "java(order.getStatus().name())")
    OrderResponseDTO toOrderResponseDTO(Order order);
}

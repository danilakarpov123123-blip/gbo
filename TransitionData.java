package ru.lemanapro.nc.logisticconditions.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.lemanapro.nc.logisticconditions.integration.out.rest.lc.dto.UOM;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
public class TransitionData {
    private String branchCode;
    private Long pcsId;
    private Integer lopusRegionId;
    private String warehouseId;
    private BigDecimal minOrderSize;
    private Integer minOrderVolume;
    private UOM minOrderVolumeType;
    private Integer minAmountRub;
    private Integer minAmountPallet;
    private Integer orderMinLeadtime;
    private BigDecimal minTransportLoadCapacity;


    public record GroupingKey(Long pcsId,
                              String warehouseId,
                              Integer lopusRegionId,
                              BigDecimal minOrderSize,
                              Integer minOrderVolume,
                              UOM minOrderVolumeType,
                              Integer minAmountRub,
                              Integer minAmountPallet,
                              Integer orderMinLeadtime){
    }
}

package ru.lemanapro.nc.logisticconditions.service;

import lombok.Data;
import ru.lemanapro.nc.logisticconditions.integration.out.rest.lc.dto.UOM;

import java.math.BigDecimal;

@Data
public class FileRowDto {
    private String branchCode;
    private Long pcsId;
    private String fias;
    private String warehouseIds;
    private BigDecimal minOrderSize;
    private Integer minOrderVolume;
    private UOM minOrderVolumeType;
    private Integer minAmountRub;
    private Integer minAmountPallet;
    private Integer orderMinLeadtime;
    private BigDecimal minTransportLoadCapacity;
}

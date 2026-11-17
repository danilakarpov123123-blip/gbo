package ru.lemanapro.nc.logisticconditions.fbo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Data
public class FboRecordData {
    private Long partnerId;
    private Long merchantId;
    private List<String> lopusRegionsIds;
    private Long productId;
    private String fboWarehouseId;
    private String merchantWarehouseId;
    private boolean fbo = false;

}

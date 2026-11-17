package ru.lemanapro.nc.logisticconditions.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.lemanapro.nc.logisticconditions.integration.out.rest.lc.dto.SupplierLogisticConditionsDto;
import ru.lemanapro.nc.logisticconditions.integration.out.rest.lc.dto.SupplierLogisticConditionsThresholdDto;
import ru.lemanapro.nc.logisticconditions.integration.out.rest.lc.dto.SupplierLogisticConditionsThresholdsDto;
import ru.lemanapro.nc.logisticconditions.integration.out.rest.lc.dto.UOM;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Component
@RequiredArgsConstructor
public class ImportFileService {
    public static final String MIN = "MIN";
    public static final String OR = "OR";
    public static final String TRANSIT = "TRANSIT";
    public static final String REGION = "REGION";
    public static final String LOCATION = "LOCATION";
    private final BranchToProductMappingService branchToProductMappingService;
    private final FiasToLopusRegionMappingService fiasToLopusRegionMappingService;
    private final ParseFileService parseFileService;

    public List<SupplierLogisticConditionsDto> importFile(InputStream stream) throws IOException {
        List<FileRowDto> fileRowDtos = parseFileService.parseFileData(stream);
        List<ImportLCDto> lcDtos = calculateData(fileRowDtos);
        return lcDtos.stream()
                .map(this::mapLogisticConditions)
                .toList();
    }

    private List<ImportLCDto> calculateData(List<FileRowDto> fileRowDtos) {
        Map<String, List<FileRowDto>> fiasErrors = new HashMap<>();
        Map<String, Integer> branchMapErrors = new HashMap<>();
        Map<TransitionData.GroupingKey, List<TransitionData>> groupedData = fileRowDtos.stream()
                .flatMap(i -> {
                    Set<Integer> lopusRegionIds = fiasToLopusRegionMappingService.getLopusRegionIdsByFias(i.getFias());
                    if (lopusRegionIds.isEmpty()) {
                        fiasErrors.compute(i.getFias(), (k, v) -> v == null ? new ArrayList<>() : v).add(i);
                    }
                    return lopusRegionIds.stream().flatMap(lopusRegionId -> map(i, lopusRegionId));
                })
                .collect(Collectors.groupingBy(d ->
                        new TransitionData.GroupingKey(
                                d.getPcsId(),
                                d.getWarehouseId(),
                                d.getLopusRegionId(),
                                d.getMinOrderSize(),
                                d.getMinOrderVolume(),
                                d.getMinOrderVolumeType(),
                                d.getMinAmountRub(),
                                d.getMinAmountPallet(),
                                d.getOrderMinLeadtime()))
                );
        List<ImportLCDto> list = new ArrayList<>();
        groupedData.forEach((k, v) -> {
            Set<Long> products = v.stream()
                    .map(TransitionData::getBranchCode)
                    .flatMap(branch -> {
                        Set<Long> productByBranchAndPartnerId = branchToProductMappingService.getProductByBranchAndPartnerId(k.pcsId(), branch);
                        if (productByBranchAndPartnerId == null || productByBranchAndPartnerId.isEmpty()) {
                            branchMapErrors.compute(k.pcsId() + "_" + branch, (pb, errCount) -> errCount == null ? 1 : errCount + 1);
                            return Stream.of();
                        }
                        return productByBranchAndPartnerId.stream();
                    }).collect(Collectors.toSet());
            list.add(new ImportLCDto(k, v, products));
        });
        fiasErrors.forEach((k, v) -> {
            System.out.println("fias: " + k + " not found, count:" + v.size());
        });
        branchMapErrors.forEach((k, v) -> {
            System.out.println(" partner + branch :" + k + " not found, count:" + v);
        });
        return list;
    }

    private SupplierLogisticConditionsDto mapLogisticConditions(ImportLCDto dto) {
        SupplierLogisticConditionsDto slcwp = new SupplierLogisticConditionsDto();
        TransitionData.GroupingKey data = dto.getKey();
        slcwp.setDeliveryLeadtime(data.orderMinLeadtime());
        slcwp.setLoadCapacity(dto.records.getFirst().getMinTransportLoadCapacity());
        slcwp.setSourceType(LOCATION);
        slcwp.setSourceId(data.warehouseId().toString());
        slcwp.setDestinationId(data.lopusRegionId().toString());
        slcwp.setDestinationType(REGION);
        slcwp.setUniquePartnerIdentifier(data.pcsId());
        slcwp.setSupplyChainCircuitId(TRANSIT);
        slcwp.setValidFrom(LocalDate.now());
        slcwp.setProductBUReferences(dto.getProductIds());
        slcwp.setThresholds(getSupplierLogisticConditionsThresholdsDto(data));
        return slcwp;
    }

    private SupplierLogisticConditionsThresholdsDto getSupplierLogisticConditionsThresholdsDto(TransitionData.GroupingKey data) {
        List<SupplierLogisticConditionsThresholdDto> thresholdList = new ArrayList<>();
        if (data.minOrderSize() != null)
            thresholdList.add(new SupplierLogisticConditionsThresholdDto(MIN, data.minOrderSize(), UOM.M3.name()));
        if (data.minOrderVolume() != null)
            thresholdList.add(new SupplierLogisticConditionsThresholdDto(MIN, toBD(data.minOrderVolume()), data.minOrderVolumeType().name()));
        if (data.minAmountRub() != null)
            thresholdList.add(new SupplierLogisticConditionsThresholdDto(MIN, toBD(data.minAmountRub()), UOM.RUB.name()));
        if (data.minAmountPallet() != null)
            thresholdList.add(new SupplierLogisticConditionsThresholdDto(MIN, toBD(data.minAmountPallet()), UOM.PAL.name()));
        if (thresholdList.isEmpty()) return null;
        return new SupplierLogisticConditionsThresholdsDto(OR, thresholdList);
    }


    private Stream<TransitionData> map(FileRowDto dto, Integer lopusRegionId) {
        String warehouseIds = dto.getWarehouseIds();
        return Stream.of(warehouseIds.split(",")).map(i ->
                new TransitionData(
                        dto.getBranchCode(),
                        dto.getPcsId(),
                        lopusRegionId,
                        i.trim(),
                        dto.getMinOrderSize(),
                        dto.getMinOrderVolume(),
                        dto.getMinOrderVolumeType(),
                        dto.getMinAmountRub(),
                        dto.getMinAmountPallet(),
                        dto.getOrderMinLeadtime(),
                        dto.getMinTransportLoadCapacity())

        );
    }

    private BigDecimal toBD(Integer val) {
        if (val == null) return null;
        return BigDecimal.valueOf(val);
    }

}

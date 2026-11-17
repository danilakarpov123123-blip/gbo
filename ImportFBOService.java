package ru.lemanapro.nc.logisticconditions.fbo;

import lombok.AllArgsConstructor;
import ru.lemanapro.nc.logisticconditions.integration.out.rest.lc.dto.SupplierLogisticConditionsDto;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
public class ImportFBOService {

    public static final String FBO = "FBO";
    public static final String REGION = "REGION";
    public static final String LOCATION = "LOCATION";

    public final FBOParseFileService fboParseFileService;

    public List<SupplierLogisticConditionsDto> processFboFile(InputStream stream) throws IOException {
        List<FboRecordData> fboRecordData = fboParseFileService.parseFileData(stream);

        Map<Long, List<FboRecordData>> groupedByPartner = fboRecordData.stream().filter(FboRecordData::isFbo)
                .collect(Collectors.groupingBy(FboRecordData::getPartnerId));

        List<SupplierLogisticConditionsDto> result = new LinkedList<>();
        groupedByPartner.forEach((k, v) -> {
                    Map<String, List<FboRecordData>> groupedByLopusRegion = getGroupedByLopusRegion(v);
                    mapToLogisticConditions(groupedByLopusRegion, result);
                }
        );
        return result;
    }

    private void mapToLogisticConditions(Map<String, List<FboRecordData>> groupedByLopusRegion, List<SupplierLogisticConditionsDto> result) {
        groupedByLopusRegion.forEach((lopusRegionId, group) -> {
            FboRecordData first = group.getFirst();
            Set<Long> collect = group.stream().map(FboRecordData::getProductId).collect(Collectors.toSet());
            SupplierLogisticConditionsDto lcDto = new SupplierLogisticConditionsDto();
            lcDto.setUniquePartnerIdentifier(first.getPartnerId());
            lcDto.setDestinationId(lopusRegionId.trim());
            lcDto.setDestinationType(REGION);
            lcDto.setSourceType(LOCATION);
            lcDto.setSupplyChainCircuitId(FBO);
            lcDto.setInitialLocationId(927L);
            lcDto.setSourceId(first.getFboWarehouseId().trim());
            lcDto.setProductBUReferences(collect);
            lcDto.setValidFrom(LocalDate.now());
            result.add(lcDto);
        });
    }

    private Map<String, List<FboRecordData>> getGroupedByLopusRegion(List<FboRecordData> records) {
        Map<String, List<FboRecordData>> groupedByLopusRegion = new HashMap<>();
        records.forEach(
                i -> {
                    if (i.isFbo()) {
                        i.getLopusRegionsIds()
                                .forEach(lopusRegionId ->
                                        groupedByLopusRegion.compute(lopusRegionId, (k, v) -> v == null ? new ArrayList<>() : v).add(i));
                    }
                }
        );
        return groupedByLopusRegion;
    }

}

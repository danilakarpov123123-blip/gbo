package ru.lemanapro.nc.logisticconditions.fbo;

import ru.lemanapro.nc.logisticconditions.integration.out.rest.lc.LogisticConditionsOutClient;
import ru.lemanapro.nc.logisticconditions.integration.out.rest.lc.LogisticConditionsOutClientImpl;
import ru.lemanapro.nc.logisticconditions.integration.out.rest.lc.dto.PageableResponse;
import ru.lemanapro.nc.logisticconditions.integration.out.rest.lc.dto.SearchRequest;
import ru.lemanapro.nc.logisticconditions.integration.out.rest.lc.dto.SupplierLogisticConditionsDto;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ImportFBO {
    public static final String fboFilePath = "";
    public static String LOG_CONDITIONS_API_URL = "";
    public static String LOG_CONDITIONS_API_USER = "";
    public static String LOG_CONDITIONS_API_PASSWORD = "";
    private static Boolean SAVE_DATA = true;

    public static void main(String[] args) throws IOException {
        FBOParseFileService fboParseFileService = new FBOParseFileService();
        ImportFBOService importFBOService = new ImportFBOService(fboParseFileService);
        List<SupplierLogisticConditionsDto> resultData = importFBOService.processFboFile(new FileInputStream(fboFilePath));
        if (SAVE_DATA) {
            LogisticConditionsOutClient client = new LogisticConditionsOutClientImpl(LOG_CONDITIONS_API_URL, LOG_CONDITIONS_API_USER, LOG_CONDITIONS_API_PASSWORD);
            Map<Long, Map<String, List<SupplierLogisticConditionsDto>>> groupedDataByPartner = getGroupedDataByPartner(client);
            for (SupplierLogisticConditionsDto slc : resultData) {
                SupplierLogisticConditionsDto existSlc = getActualForUpdate(slc, groupedDataByPartner);
                if (existSlc == null) {
                    client.create(slc);
                    System.out.println("create : " + slc);
                } else {
                    existSlc.setValidTo(null);
                    existSlc.setProductBUReferences(slc.getProductBUReferences());
                    existSlc.setInitialLocationId(slc.getInitialLocationId());
                    Long currentId = existSlc.getId();
                    existSlc.setId(null);
                    client.update(currentId, existSlc);
                    System.out.println("update supplierLogisticConditionsId:" + currentId + "data: " + existSlc);
                }
            }

            groupedDataByPartner.forEach((k, v) ->
                    v.values().forEach(i -> i.forEach(logCondition -> {
                        if(logCondition.getValidTo() == null || logCondition.getValidTo().isAfter(LocalDate.now())) {
                            logCondition.setValidTo(LocalDate.now().minusDays(1));
                            logCondition.setProductBUReferences(Set.of());
                            client.update(logCondition.getId(), logCondition);
                        }
                    })));
        }
    }

    private static SupplierLogisticConditionsDto getActualForUpdate(SupplierLogisticConditionsDto slc, Map<Long, Map<String, List<SupplierLogisticConditionsDto>>> groupedDataByPartner) {
        Map<String, List<SupplierLogisticConditionsDto>> groupByPartner = groupedDataByPartner.get(slc.getUniquePartnerIdentifier());
        if (groupByPartner == null) return null;
        List<SupplierLogisticConditionsDto> dtoListForLopus = groupByPartner.get(slc.getDestinationId());
        if (dtoListForLopus == null || dtoListForLopus.isEmpty()) return null;
        return dtoListForLopus.removeFirst();
    }

    private static Map<Long, Map<String, List<SupplierLogisticConditionsDto>>> getGroupedDataByPartner(LogisticConditionsOutClient client) {
        PageableResponse<SupplierLogisticConditionsDto> allResultFBO = client.search(new SearchRequest("LOCATION", "262", "FBO"));
        return allResultFBO.getContent().stream()
                .collect(Collectors.groupingBy(SupplierLogisticConditionsDto::getUniquePartnerIdentifier,
                        Collectors.groupingBy(SupplierLogisticConditionsDto::getDestinationId)));
    }

}

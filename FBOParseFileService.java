package ru.lemanapro.nc.logisticconditions.fbo;

import org.apache.logging.log4j.util.Strings;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class FBOParseFileService {

    public static final String FBO = "FBO";

    public List<FboRecordData> parseFileData(InputStream stream) throws IOException {
        Workbook workbook = new XSSFWorkbook(stream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();
        rowIterator.next();//skip headers
        List<FboRecordData> fileRowDtos = new ArrayList<>();
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            FboRecordData rowRecord = read(row);
            if (rowRecord != null) {
                fileRowDtos.add(rowRecord);
            } else break;
        }
        return fileRowDtos;
    }

    private FboRecordData read(Row row) {
        FboRecordData rowDto = new FboRecordData();
        Long partnerId = getLongValue(row, 0);
        if (partnerId == null) {
            return null;
        }
        rowDto.setPartnerId(partnerId);
        rowDto.setMerchantId(getLongValue(row, 1));
        rowDto.setFboWarehouseId(getStringValue(row, 2));
        rowDto.setMerchantWarehouseId(getStringValue(row, 3));
        rowDto.setLopusRegionsIds(Arrays.asList(getStringValue(row, 4).split(",")));
        rowDto.setProductId(getLongValue(row, 5));
        String scheme = getStringValue(row, 10);
        if (scheme != null) {
            rowDto.setFbo(scheme.contains(FBO));
        }
        return rowDto;
    }

    private String getStringValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue();
    }

    private Integer getIntValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        cell.setCellType(CellType.STRING);
        String stringCellValue = cell.getStringCellValue();
        if (Strings.isNotBlank(stringCellValue)) return Integer.valueOf(stringCellValue.replace(" ", ""));
        return null;
    }

    private BigDecimal getBigDecimal(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        cell.setCellType(CellType.STRING);
        String stringCellValue = cell.getStringCellValue();
        if (Strings.isNotBlank(stringCellValue)) return new BigDecimal(stringCellValue.replace(",", "."));
        return null;
    }

    private Long getLongValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        cell.setCellType(CellType.STRING);
        String stringCellValue = cell.getStringCellValue();
        if (Strings.isNotBlank(stringCellValue)) return Long.valueOf(stringCellValue);
        return null;
    }
}

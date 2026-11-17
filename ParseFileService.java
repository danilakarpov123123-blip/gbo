package ru.lemanapro.nc.logisticconditions.service;

import org.apache.logging.log4j.util.Strings;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.lemanapro.nc.logisticconditions.integration.out.rest.lc.dto.UOM;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ParseFileService {

    public List<FileRowDto> parseFileData(InputStream stream) throws IOException {
        Workbook workbook = new XSSFWorkbook(stream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();
        rowIterator.next();//skip headers
        List<FileRowDto> fileRowDtos = new ArrayList<>();
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            FileRowDto rowRecord = read(row);
            if (rowRecord != null) {
                fileRowDtos.add(rowRecord);
            }
        }
        return fileRowDtos;
    }

    private FileRowDto read(Row row) {
        FileRowDto fileRowDto = new FileRowDto();
        Long branch = getLongValue(row, 0);
        if (branch == null) {
            return null;
        }
        fileRowDto.setBranchCode(branch.toString());
        fileRowDto.setPcsId(getLongValue(row, 1));
        fileRowDto.setFias(getStringValue(row, 2));
        fileRowDto.setWarehouseIds(getStringValue(row, 3));
        fileRowDto.setMinTransportLoadCapacity(getBigDecimal(row, 4));
        fileRowDto.setMinOrderSize(getBigDecimal(row, 5));
        Integer minAmount = getIntValue(row, 6);
        fileRowDto.setMinOrderVolume(minAmount);
        if(minAmount != null) {
            fileRowDto.setMinOrderVolumeType(UOM.forValue(getStringValue(row, 7)));
        }
        fileRowDto.setMinAmountRub(getIntValue(row, 8));
        fileRowDto.setMinAmountPallet(getIntValue(row, 9));
        fileRowDto.setOrderMinLeadtime(getIntValue(row, 10));
        return fileRowDto;
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
        if (Strings.isNotBlank(stringCellValue)) return Integer.valueOf(stringCellValue.replace(" ",""));
        return null;
    }

    private BigDecimal getBigDecimal(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        cell.setCellType(CellType.STRING);
        String stringCellValue = cell.getStringCellValue();
        if (Strings.isNotBlank(stringCellValue)) return new BigDecimal(stringCellValue.replace(",","."));
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

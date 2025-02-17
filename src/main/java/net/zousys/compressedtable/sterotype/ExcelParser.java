package net.zousys.compressedtable.sterotype;

import lombok.Builder;
import net.zousys.compressedtable.CompressedTableFactory;
import net.zousys.compressedtable.impl.CompressedTable;
import net.zousys.compressedtable.impl.KeyHeadersList;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Builder
/**
 * The Excel headers should be unique, the duplicated header will cause header missing
 */
public class ExcelParser {
    private boolean dynamicWidth;
    private int headerPosition;
    @Builder.Default
    private int ignoredTailLines = 0;
    @Builder.Default
    private KeyHeadersList keyHeaderList = new KeyHeadersList();
    @Builder.Default
    private boolean compressed = true;

    /**
     * @param inputStream
     * @return
     * @throws IOException
     */
    public CompressedTable parse(InputStream inputStream) throws IOException {
        if (inputStream != null) {
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = workbook.getSheetAt(0);
            int from = sheet.getFirstRowNum();
            int to = sheet.getLastRowNum();

            CompressedTable compressedTable = new CompressedTable(
                    keyHeaderList.getKeyHeadersList().size()>1?
                            CompressedTableFactory.Mode.MULTI_KEYS:
                            CompressedTableFactory.Mode.SINGLE_KEY);
            compressedTable.setCompressed(compressed);
            compressedTable.setHeaderRowNumber(headerPosition);
            if (keyHeaderList != null) {
                compressedTable.setKeyHeaderList(keyHeaderList);
            }

            int columnNo = -1;

            for (int i = from; i <= to; i++) {
                ArrayList<String> arowarray = new ArrayList<>();
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(i);
                if (row!=null) {
                    int cn = row.getPhysicalNumberOfCells();

                    for (int j = 0; j < (columnNo == -1 ? cn : columnNo); j++) {
                        arowarray.add(stringvalue(row.getCell(j, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)).trim());
                    }

                    if (headerPosition == i) {
                        columnNo = cn;
                        compressedTable.setHeaders(arowarray);
                    } else {
                        compressedTable.appendRow(arowarray);
                    }
                } else {
                    // empty row
                }
            }
            return compressedTable;
        }
        return null;
    }

    /**
     * @param cell
     * @return
     */
    private String stringvalue(Cell cell) {
        if (cell==null) {
            return "";
        }
        CellType type = cell.getCellType();
        if (type == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toString();
            }
            return "" + cell.getNumericCellValue();
        } else if (type == CellType.FORMULA) {
            if (cell.getCachedFormulaResultType() == CellType.NUMERIC) {
                return ""+cell.getNumericCellValue();
            } else if (cell.getCachedFormulaResultType() == CellType.STRING) {
                return cell.getRichStringCellValue().getString();
            }
            return cell.getCellFormula();
        } else if (type == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (type == CellType.BOOLEAN) {
            return ""+cell.getBooleanCellValue();
        } else {
            return cell.getStringCellValue();
        }
    }

}

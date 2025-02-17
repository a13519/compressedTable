package net.zousys.compressedtable.sterotype;

import lombok.Builder;
import net.zousys.compressedtable.CompressedTableFactory;
import net.zousys.compressedtable.impl.KeyHeaders;
import net.zousys.compressedtable.impl.CompressedTable;
import net.zousys.compressedtable.impl.KeyHeadersList;
import org.apache.commons.csv.CSVFormat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

@Builder
/**
 *
 */
public class CSVParser {
    @Builder.Default
    private int ignoredTailLines = 0;
    @Builder.Default
    private KeyHeadersList keyHeaderList = new KeyHeadersList();
    @Builder.Default
    private char delimeter = ',';
    private int headerPosition;
    @Builder.Default
    private boolean compressed = true;


    /**
     *
     * @param headers
     * @return
     */
    public CSVParser addKeyHeaders(KeyHeaders headers) {
        keyHeaderList.addHeaders(headers);
        return this;
    }

    /**
     * @param inputStream
     * @return
     * @throws IOException
     */
    public CompressedTable parse(InputStream inputStream) throws IOException {
        if (inputStream != null) {
            Reader in = new BufferedReader(new InputStreamReader(inputStream));
            CSVFormat format = CSVFormat.RFC4180.builder()
                    .setDelimiter(delimeter)
                    .setIgnoreEmptyLines(true)
                    .setIgnoreSurroundingSpaces(true)
                    .setTrim(true)
                    .build();
            CompressedTable compressedTable = new CompressedTable(
                    keyHeaderList.getKeyHeadersList().size()==1?
                            CompressedTableFactory.Mode.SINGLE_KEY:
                            CompressedTableFactory.Mode.MULTI_KEYS);
            compressedTable.setCompressed(compressed);
            compressedTable.setHeaderRowNumber(headerPosition);
            if (keyHeaderList != null) {
                compressedTable.setKeyHeaderList(keyHeaderList);
            }
            AtomicInteger n = new AtomicInteger();
            format.parse(in).stream().forEach(re -> {
                try {
                    if (n.get() >= headerPosition) {
                        if (re.values() != null) {
                            String[] fields = re.values();
                            compressedTable.appendRow(
                                    fields,
                                    n.get() == headerPosition);
                        }
                    }
                } catch (IOException e) {
                    //
                }
                n.getAndIncrement();
            });

            return compressedTable;
        }
        return null;
    }

    /**
     * @param cell
     * @return
     */
    private String stringvalue(Cell cell) {
        CellType type = cell.getCellType();
        switch (type) {
            case NUMERIC: {
                return "" + cell.getNumericCellValue();
            }
            case FORMULA: {
                return cell.getCellFormula();
            }
            default: {
                return cell.getStringCellValue();
            }
        }
    }
}

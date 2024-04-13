package com.requirementyogi.datacenter.psea.impl;

import com.google.common.collect.Lists;
import com.requirementyogi.datacenter.psea.api.Row;
import com.requirementyogi.datacenter.psea.api.Sheet;
import com.requirementyogi.datacenter.psea.api.Value;
import com.requirementyogi.datacenter.psea.api.WorkbookAPI;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class CreateExcelFileTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateExcelFileTest.class);
    private static int fileNumber = 1;

    private final PseaTestUtils utils = new PseaTestUtils();

    @Test
    public void test() throws IOException {
        PseaServiceImpl psea = utils.psea;
        File file = psea.export(workbookAPI -> {
            Row row;
            Sheet sheet = workbookAPI.newSheet("ABC");
            sheet.addRow(Lists.newArrayList(
                    new Value("1. Please check the following items:")
            ));
            sheet.addRow(Lists.newArrayList(
                    new Value(null),
                    new Value("2. <- Previous cell contains 'null'"),
                    null,
                    new Value("4. This is in column 4, line 3")
            ));
            sheet.addRow(Lists.newArrayList(
                    new Value(WorkbookAPI.Style.TH, "4. This is a <th>"),
                    new Value(WorkbookAPI.Style.ID_COLUMN, "5. This is a <th> (for an ID, but it isn't visible)"),
                    new Value(WorkbookAPI.Style.ERROR_CELL, "6. This is red text"),
                    new Value(WorkbookAPI.Style.RED_CELL, "7. This is on red background"),
                    new Value(WorkbookAPI.Style.MIRROR_CELL, "8. This cell is on gray background")
            ));
            sheet.addRow(Lists.newArrayList(
                    new Value("9. Next row is empty"),
                    new Value(WorkbookAPI.Style.WORKBOOK_TITLE, "10. This is a big title (vertically centered, bold, font 16, height of row is 60px)")
            ));
            sheet.setHeightInPoints(4, 60);
            sheet.addRow(Lists.newArrayList());
            row = sheet.addRow(Lists.newArrayList());
            row.setCell(5, new Value("11. This is in the 6th column."));
            sheet.addRow(0, Lists.newArrayList(
                    new Value("12. Top-left cell"),
                    new Value("13. Second cell in the headers")
                    )
            );
            sheet.addRow(10, Lists.newArrayList(
                    new Value("14. This is merged with number 15: "),
                    new Value("15. (see 14)")
                    )
            );
            sheet.addMergedRegion(10, 10, 0, 1);

            sheet.addRow(12, Lists.newArrayList(
                    new Value("16. Single cell"),
                    new Value("18. This is a 4x4 merged cell with 16 and 17 on the left. Numbers 19-21 should not appear."),
                    new Value("19. Should not appear")
                    )
            );
            sheet.addRow(13, Lists.newArrayList(
                    new Value("17. Single cell"),
                    new Value("20. Should not appear"),
                    new Value("21. Should not appear")
                    )
            );
            sheet.addMergedRegion(12, 13, 1, 2);
            sheet.addRow(Lists.newArrayList(
                    new Value("22. Columns should have the correct width"),
                    new Value("23. 2 columns should be frozen on the left"),
                    new Value("24. 3 rows should be frozen at the top")
            ));
            sheet.autoSizeHeaders();
            sheet.freezePanes(2, 3, 2, 3);
            sheet.addRow(Lists.newArrayList(
                    new Value(null, "25. This should be a link to example-dot-com", "https://example.com")
            ));
            sheet.setRowNum(28);
            sheet.addRow(Lists.newArrayList(
                    new Value("26. This is row 29"),
                    new Value("27. There is a second sheet named \"Helicopter\""),
                    new Value("28. One Korean character appears: \uD79D"),
                    new Value("29. This is a euro sign: €"),
                    new Value("30. There are 30 items.")
            ));

            workbookAPI.newSheet("Helicopter");
        });

        File targetFile = writeToTarget(file);
        assertTrue(targetFile.exists());
    }

    private File writeToTarget(File file) throws IOException {
        if (!new File("target").isDirectory()) {
            throw new IllegalStateException("We can't find a 'target' directory in the working directory." +
                    " Please locate yourself in the root of the project or module.");
        }
        File targetFile;
        do {
            targetFile = new File("target", "file-" + ++fileNumber + ".xlsx");
        } while (targetFile.exists());
        FileUtils.copyFile(file, targetFile);
        LOGGER.info("Excel file is in " + targetFile.getAbsolutePath() + ", please check its contents.");
        return targetFile;
    }
}

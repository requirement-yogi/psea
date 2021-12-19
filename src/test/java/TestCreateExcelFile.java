import com.google.common.collect.Lists;
import com.playsql.psea.api.Sheet;
import com.playsql.psea.api.Value;
import com.playsql.psea.impl.PseaServiceImpl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class TestCreateExcelFile {

    private static int fileNumber = 1;

    @Test
    public void test() throws IOException {
        PseaServiceImpl psea = new PseaServiceImpl();
        File file = psea.export(workbookAPI -> {
            Sheet sheet = workbookAPI.newSheet("ABC");
            sheet.addRow(Lists.newArrayList(
                    new Value("Test sheet 1")
            ));
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
        return targetFile;
    }
}

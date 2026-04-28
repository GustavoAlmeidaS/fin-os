import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import java.io.FileInputStream;
public class ReadXls {
    public static void main(String[] args) throws Exception {
        Workbook wb = new HSSFWorkbook(new FileInputStream("docs/ExtratoSantander.xls"));
        Sheet sheet = wb.getSheetAt(0);
        for (int i=0; i<15; i++) {
            Row r = sheet.getRow(i);
            if(r != null) {
                System.out.print("Row " + i + ": ");
                for (int c=0; c<6; c++) {
                    Cell cell = r.getCell(c);
                    if(cell != null) {
                        if(cell.getCellType() == CellType.STRING) System.out.print("[" + cell.getStringCellValue() + "] ");
                        else if(cell.getCellType() == CellType.NUMERIC) System.out.print("[" + cell.getNumericCellValue() + "] ");
                    }
                }
                System.out.println();
            }
        }
    }
}

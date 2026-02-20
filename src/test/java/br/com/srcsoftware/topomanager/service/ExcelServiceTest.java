package br.com.srcsoftware.topomanager.service;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import br.com.srcsoftware.topomanager.repository.RegistroRepository;

@SpringBootTest
class ExcelServiceTest {

    @Autowired
    private ExcelService excelService;

    @MockBean
    private RegistroRepository repository;

    @Test
    @DisplayName("Deve processar arquivo excel e chamar o repositório")
    void deveProcessarExcelComSucesso() throws Exception {
        // Mock de um arquivo Excel em memória para teste
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet();
        Row header = sheet.createRow(0);
        Row data = sheet.createRow(1);
        data.createCell(0).setCellValue(10); // Coluna A
        data.createCell(1).setCellValue(20); // Coluna B
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        InputStream is = new ByteArrayInputStream(bos.toByteArray());

        excelService.importar(is);

        verify(repository, atLeastOnce()).saveAll(anyList());
    }
}

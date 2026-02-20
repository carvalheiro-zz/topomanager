package br.com.srcsoftware.topomanager.service;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import br.com.srcsoftware.topomanager.model.po.RegistroExcel;
import br.com.srcsoftware.topomanager.repository.RegistroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportarExcelService {

    private final RegistroRepository repository;

    public byte[] gerarRelatorioExcel() throws Exception {
        List<RegistroExcel> dados = repository.findFiltroCustomizado();

        log.info("Iniciando o processo de geração do relatorio.");
        
        if (dados == null || dados.isEmpty()) {
            throw new RuntimeException("Nenhum registro encontrado para os critérios selecionados.");
        }
        
        // 1. Criamos o Stream de saída fora do try para garantir que ele sobreviva
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("Resultado");
            
         // Cabeçalho
            Row header = sheet.createRow(0);
            String[] colunas = {"ID", "Coluna A", "Coluna B", "C", "D", "E", "F", "G"};
            for (int i = 0; i < colunas.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(colunas[i]);
            }

            // Dados
            int rowNum = 1;
            for (RegistroExcel registro : dados) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(registro.getId());
                row.createCell(1).setCellValue(registro.getColunaA());
                row.createCell(2).setCellValue(registro.getColunaB());
                row.createCell(3).setCellValue(registro.getColunaC());
                row.createCell(4).setCellValue(registro.getColunaD());
                row.createCell(5).setCellValue(registro.getColunaE());
                row.createCell(6).setCellValue(registro.getColunaF());
                row.createCell(7).setCellValue(registro.getColunaG());
            }
            
            // 2. Escrevemos no stream enquanto o workbook ainda está "vivo"
            workbook.write(out);
            
            // 3. Importante: Limpar os arquivos temporários que o SXSSF cria no disco
            workbook.dispose(); 
        } 
        
        // 4. Retornamos o array de bytes APÓS o fechamento do workbook
        return out.toByteArray();
    }
}
package br.com.srcsoftware.topomanager.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;

import br.com.srcsoftware.topomanager.model.po.NumerosNegativos;
import br.com.srcsoftware.topomanager.model.po.NumerosPositivos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelService {

	public List<NumerosPositivos> importar210(Workbook workbook/*InputStream is*/) throws Exception {
		List<NumerosPositivos> listaPositivosParaSalvar = new ArrayList<>();

		log.info("Iniciando processamento da planilha 210 (Positivos)");
		
			Sheet sheet = workbook.getSheetAt(0);

			for (Row row : sheet) {
				if (row == null) break;

				if (row.getRowNum() == 0) continue; // Pula cabeçalho
				
				// 2. Pega a Célula A (índice 0)
	            Cell cellA = row.getCell(0);
				// 3. CONDIÇÃO DE PARADA: Se a célula A estiver vazia ou sem valor
	            if (cellA == null || cellA.getCellType() == CellType.BLANK) {
	                log.info("Leitura interrompida: Celula A está vazia.");
	                break; // Sai do laço 'for' e encerra a leitura da planilha
	            }
				
				//log.info("Processando linha: {} de {}", row.getRowNum(), sheet.getLastRowNum());
				NumerosPositivos registro = NumerosPositivos.builder()
						.colunaA((int) row.getCell(0).getNumericCellValue())
						.colunaB((int) row.getCell(1).getNumericCellValue())
						.colunaC((int) row.getCell(2).getNumericCellValue())
						.colunaD((int) row.getCell(3).getNumericCellValue())
						.colunaE((int) row.getCell(4).getNumericCellValue())
						.colunaF((int) row.getCell(5).getNumericCellValue())

						.build();

				listaPositivosParaSalvar.add(registro);
			}
		
			log.info("Linhas processadas da planilha 210 (Positivos): {}", listaPositivosParaSalvar.size());
		return listaPositivosParaSalvar;
	}
	
	public List<NumerosNegativos> importar5005(Workbook workbook/*InputStream is*/) throws Exception {
		List<NumerosNegativos> listaNegativosParaSalvar = new ArrayList<>();

		log.info("Iniciando processamento da planilha 5005 (Negativos)");
		
			Sheet sheet = workbook.getSheetAt(0);

			for (Row row : sheet) {
				if (row.getRowNum() == 0) continue; // Pula cabeçalho
				//log.info("Processando linha: {} de {}", row.getRowNum(), sheet.getLastRowNum());
				NumerosNegativos registro = NumerosNegativos.builder()
						.colunaH((int) row.getCell(7).getNumericCellValue())
						.colunaI((int) row.getCell(8).getNumericCellValue())
						.colunaJ((int) row.getCell(9).getNumericCellValue())
						.colunaK((int) row.getCell(10).getNumericCellValue())
						.colunaL((int) row.getCell(11).getNumericCellValue())
						.colunaM((int) row.getCell(12).getNumericCellValue())

						.build();

				listaNegativosParaSalvar.add(registro);
			}
		
			log.info("Linhas processadas da planilha 5005 (Negativos): {}", listaNegativosParaSalvar.size());
		return listaNegativosParaSalvar;
	}
}
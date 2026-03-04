package br.com.srcsoftware.topomanager.controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.srcsoftware.topomanager.controller.response.RespostaProcessamento;
import br.com.srcsoftware.topomanager.model.pojo.GrupoPOJO;
import br.com.srcsoftware.topomanager.service.IdentificadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "*") // Permite que o HTML acesse a API
@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
@Tag(name = "Importação", description = "Endpoint para upload e processamento de planilhas")
public class ExcelController {
    
    private final IdentificadorService identificadorService;
  
    
    @PostMapping(value = "/processar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Realiza o upload de uma planilha Excel ( CENARIO 1 )", 
               description = "O arquivo deve conter as colunas A a F, sendo todas números inteiros.")
    public ResponseEntity<?> processar(
            @Parameter(description = "Arquivo .xlsx ou .xls") 
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam("cadeia") String cadeiaNumeros) {
        
    	StopWatch watch = new StopWatch();
        watch.start();
        
        if (arquivo.isEmpty()) {
            return ResponseEntity.badRequest().body("Por favor, selecione um arquivo.");
        }
        
        if (!arquivo.getContentType().equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("Somente arquivos .xlsx são permitidos.");
        }

        try {
        	// 1. Você pode limpar a String aqui também por segurança
            String cadeiaLimpa = cadeiaNumeros.replaceAll("[^0-9,]", "");
            
            // 2. Converter a String "1,4,7,8" em uma lista de Inteiros
            List<Integer> listaDos15Numeros = Arrays.stream(cadeiaLimpa.split(","))
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
            
            
        	List<GrupoPOJO> resultado = identificadorService.processarCenario1(arquivo.getInputStream(), listaDos15Numeros );
            
            watch.stop();
            
            // Pegamos o tempo total formatado (ex: "1.25s")
            String tempoFormatado = String.format("%.2f s", watch.getTotalTimeSeconds());
            
            return ResponseEntity.ok(new RespostaProcessamento("Processamento concluído com sucesso!", tempoFormatado, resultado.size()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Erro ao processar planilha: " + e.getMessage());
        }
    }
    
    // Caso queira gerar um Excel.
    /*("/download-resultados")
    public ResponseEntity<byte[]> downloadResultados() throws IOException {
        // 1. Obter a lista de combinações (gerada previamente ou gerada agora)
        List<CombinacaoResultado> resultados = servico.gerarListaDeCombinacoesOtimizada();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Combinações 15 Números");

            // Cabeçalho
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID Grupo");
            for (int i = 1; i <= 15; i++) {
                header.createCell(i).setCellValue("N" + i);
            }

            // Dados
            int rowIdx = 1;
            for (CombinacaoResultado res : resultados) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(res.getGrupoId());

                int colIdx = 1;
                for (Integer num : res.getNumeros()) {
                    row.createCell(colIdx++).setCellValue(num);
                }
            }

            workbook.write(out);
            
            byte[] conteudo = out.toByteArray();
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resultados_combinados.xlsx");
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(conteudo);
        }
    }*/
}
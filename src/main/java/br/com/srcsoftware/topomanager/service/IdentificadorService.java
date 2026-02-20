package br.com.srcsoftware.topomanager.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet; // Importado para manter a ordem
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import br.com.srcsoftware.topomanager.constantes.Grupos;
import br.com.srcsoftware.topomanager.model.po.NumerosNegativos;
import br.com.srcsoftware.topomanager.model.po.NumerosPositivos;
import br.com.srcsoftware.topomanager.model.pojo.GrupoPOJO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentificadorService {

    private final ExcelService excelService;

    public List<GrupoPOJO> processarCenario1(InputStream is, List<Integer> listaDos15Numeros) throws Exception {
        StopWatch watch = new StopWatch("Processamento TopoManager");
        
        try {            
            List<NumerosPositivos> positivos = new ArrayList<>();
            List<NumerosNegativos> negativos = new ArrayList<>();
            
            watch.start("Recuperando da planilha todos os numeros Positivos e Negativos");
            try (Workbook workbook = new XSSFWorkbook(is)) {
                positivos = excelService.importar210(workbook);                
                negativos = excelService.importar5005(workbook);
            }
            watch.stop();
            
            // Alterado para Map<String, LinkedHashSet<LinkedHashSet<Integer>>>
            Map<String, LinkedHashSet<LinkedHashSet<Integer>>> gruposComPositivosEncontrados = new HashMap<>();
            
            watch.start("Separando todos os Grupos que possuam todos os Positivos.");
            for(NumerosPositivos numeros : positivos) {
                buscarGruposCompativeis(gruposComPositivosEncontrados, Grupos.grupos, numeros.toArrayList());                
            }             
            watch.stop();
            for( Entry<String, LinkedHashSet<LinkedHashSet<Integer>>> grupoCorrente : gruposComPositivosEncontrados.entrySet()) {
            	log.info("{}",grupoCorrente);
            }
            
            // Mantendo a ordem de remoção com LinkedHashSet
            LinkedHashSet<List<Integer>> resultado9Restantes = new LinkedHashSet<>();
            
            watch.start("Obtendo uma lista com 9 numeros.");
            for(NumerosNegativos numeros : negativos) {                
                resultado9Restantes.add(obterOs9Resultantes(listaDos15Numeros, numeros.toArrayList()));                
            }    
            watch.stop(); 
                       
            Map<String, LinkedHashSet<LinkedHashSet<Integer>>> gruposComOs9Encontrados = new HashMap<>();
                        
            watch.start("Separando todos os Grupos que possuam todos os 9.");
            for(List<Integer> os9 : resultado9Restantes) {                
                buscarGruposCompativeis(gruposComOs9Encontrados, Grupos.grupos, os9);                
            }
            watch.stop();
            for( Entry<String, LinkedHashSet<LinkedHashSet<Integer>>> grupoCorrente : gruposComOs9Encontrados.entrySet()) {
            	log.info("{}",grupoCorrente);
            }
                                    
            watch.start("Iniciando o processo de combinação dos Grupos");
            List<GrupoPOJO> resultadoUnificadoGeral = gerarListaDeCombinacoes(gruposComPositivosEncontrados, gruposComOs9Encontrados);
            watch.stop();
            
            List<GrupoPOJO> encontrados1540 = filtrarPorRepeticaoDeId(resultadoUnificadoGeral, 1540);
            
            log.info(watch.prettyPrint());
            log.info("Tempo total: {} segundos.", watch.getTotalTimeSeconds());
            
            exportarExcelParaDisco(encontrados1540);
            
            return encontrados1540;
        } catch (Exception e) {
            log.error("Erro no processamento do Cenário 1", e);
            throw e;
        }
    }
    
    public List<GrupoPOJO> gerarListaDeCombinacoes(
            Map<String, LinkedHashSet<LinkedHashSet<Integer>>> gruposComPositivosEncontrados, 
            Map<String, LinkedHashSet<LinkedHashSet<Integer>>> gruposComOs9Encontrados) {
        
        List<GrupoPOJO> listaFinal = new ArrayList<>();

        gruposComPositivosEncontrados.forEach((chave, setsDe6) -> {
            if (gruposComOs9Encontrados.containsKey(chave)) {
                LinkedHashSet<LinkedHashSet<Integer>> setsDe9 = gruposComOs9Encontrados.get(chave);

                for (LinkedHashSet<Integer> jogo6 : setsDe6) {
                    for (LinkedHashSet<Integer> jogo9 : setsDe9) {
                        
                        // LinkedHashSet garante que jogo6 venha antes de jogo9 na ordem de inserção
                        Set<Integer> uniao15 = new LinkedHashSet<>(jogo6);
                        uniao15.addAll(jogo9);
                        
                        listaFinal.add(new GrupoPOJO(chave, uniao15));
                    }
                }
            }
        });

        return listaFinal;
    }
    
    public void buscarGruposCompativeis(
            Map<String, LinkedHashSet<LinkedHashSet<Integer>>> gruposEncontrados, 
            Map<String, LinkedHashSet<Integer>> gruposBase, 
            List<Integer> numerosIdentificar) {

        for (Map.Entry<String, LinkedHashSet<Integer>> entrada : gruposBase.entrySet()) {
            String idGrupo = entrada.getKey();
            LinkedHashSet<Integer> numerosDoGrupo = entrada.getValue();            

            if (numerosDoGrupo.containsAll(numerosIdentificar)) {        
                LinkedHashSet<Integer> numerosNovoGrupo = new LinkedHashSet<>(numerosIdentificar);
                
                // Uso de computeIfAbsent para simplificar e garantir LinkedHashSet
                gruposEncontrados.computeIfAbsent(idGrupo, k -> new LinkedHashSet<>())
                                 .add(numerosNovoGrupo);
            }
        }        
    }
    
    public List<Integer> obterOs9Resultantes(List<Integer> baseDe15Numeros, List<Integer> negativos) {
        Set<Integer> resultado = new LinkedHashSet<>(baseDe15Numeros);
        resultado.removeAll(negativos);
        return new ArrayList<>(resultado);
    }
    
    public List<GrupoPOJO> filtrarPorRepeticaoDeId(List<GrupoPOJO> resultadoUnificadoGeral, int frequenciaAlvo) {
        Map<String, List<GrupoPOJO>> agrupadoPorId = resultadoUnificadoGeral.stream()
                .collect(Collectors.groupingBy(GrupoPOJO::getGrupoId));

        return agrupadoPorId.values().stream()
                .filter(lista -> lista.size() == frequenciaAlvo)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public String exportarExcelParaDisco(List<GrupoPOJO> resultados) throws IOException {
        log.info("Exportando o resultado parcial em um XLS.");
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
        String caminhoInternoContainer = "/app/temp"; 
        String nomeArquivo = "grupos_repetidos_1540x_" + timestamp + ".xlsx";
        String caminhoCompleto = caminhoInternoContainer + "/" + nomeArquivo;

        Files.createDirectories(Paths.get(caminhoInternoContainer));

        try (Workbook workbook = new XSSFWorkbook(); 
             FileOutputStream fileOut = new FileOutputStream(caminhoCompleto)) {
            
            Sheet sheet = workbook.createSheet("Grupos 1540");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID Grupo");
            for (int i = 1; i <= 15; i++) {
                header.createCell(i).setCellValue("N" + i);
            }

            int rowIdx = 1;
            for (GrupoPOJO res : resultados) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(res.getGrupoId());

                int colIdx = 1;
                // Os números já vêm ordenados pela ordem de inserção devido ao LinkedHashSet
                for (Integer num : res.getNumeros()) {
                    row.createCell(colIdx++).setCellValue(num);
                }
            }

            workbook.write(fileOut);
            log.info("✅ Arquivo gravado com sucesso: {}", caminhoCompleto);
            return nomeArquivo;
        }
    }    
}

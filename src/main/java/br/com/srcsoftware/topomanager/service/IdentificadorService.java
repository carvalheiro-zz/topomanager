package br.com.srcsoftware.topomanager.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

/**
 * Os 15 - 2,3,5,6,9,10,11,13,14,16,18,20,23,24,25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdentificadorService {

	private final ExcelService excelService;

	/**
	 * Cenario 1
	 * Identificar todos os Grupos de 19 numeros que possuam todos os numeros 'Positivos'(210)
	 * Pegar os Grupos identificados e separar novamente apenas os que possuam todos os numeros 'Negativos'(5005).
	 * @param is
	 * @throws Exception 
	 */
	public List<GrupoPOJO> processarCenario1(InputStream is, List<Integer> listaDos15Numeros) throws Exception {
		StopWatch watch = new StopWatch("Processamento TopoManager");
		
		try {			
			
			List<NumerosPositivos> positivos = new ArrayList<>();
			List<NumerosNegativos> negativos = new ArrayList<>();
			
			watch.start("Recuperando da planilha todos os numeros Positivos e Negativos");
			try (Workbook workbook = new XSSFWorkbook(is)) {
				log.info("Recuperando da planilha todos os numeros Positivos (210)");			
				positivos = excelService.importar210(workbook);				
				log.info("Recuperando da planilha todos os numeros Negativos (5005)");
				negativos = excelService.importar5005(workbook);
			}
			watch.stop();
			
			// Grupos com os 210 Positivos
			HashMap<String, HashSet<HashSet<Integer>>> gruposComPositivosEncontrados = new HashMap<>();
			
			watch.start("Separando todos os Grupos que possuam todos os Positivos.");
			log.info("Separando todos os Grupos que possuam todos os Positivos.");
			for(NumerosPositivos numeros: positivos) {
				buscarGruposCompativeis(gruposComPositivosEncontrados, Grupos.grupos, numeros.toArrayList());				
			}						
			log.info("Grupos encontrados com os POSITIVOS: Qtd:{}", gruposComPositivosEncontrados.size());
			watch.stop();
			
			// Pegando os 9 dos 15
			HashSet<List<Integer>> resultado9Restantes = new HashSet<>();
			
			watch.start("Obtendo uma lista com 9 numeros.");
			log.info("Obtendo uma lista com 9 numeros com base na remocao de todos os Negativos da Base de 15.");
			for(NumerosNegativos numeros: negativos) {				
				resultado9Restantes.add( obterOs9Resultantes(listaDos15Numeros, numeros.toArrayList()) );				
			}	
			log.info("Os 9 gerados a partir dos 15: Qtd:{}", resultado9Restantes.size());
			watch.stop();
			
			// Grupos com os 5005 de 9
			HashMap<String, HashSet<HashSet<Integer>>> gruposComOs9Encontrados = new HashMap<>();
						
			log.info("Separando todos os Grupos que possuam todos os 9.");			
			watch.start("Separando todos os Grupos que possuam todos os 9.");
			for(List<Integer> os9 : resultado9Restantes) {				
				buscarGruposCompativeis(gruposComOs9Encontrados, Grupos.grupos, os9);				
			}
			log.info("Grupos encontrados com os 9: Qtd:{}", gruposComOs9Encontrados.size());
			watch.stop();
									
			// Unificando TODOS os numeros encontrados nas 2 lista (dos positivos e dos 9) por grupo.
			watch.start("Iniciando o processo de combinação dos Grupos");
			log.info("Iniciando o processo de combinação dos Grupos");
			List<GrupoPOJO> resultadoUnificadoGeral = gerarListaDeCombinacoes(gruposComPositivosEncontrados, gruposComOs9Encontrados);
			log.info("Finalizando o processo de combinação dos Grupos: Qtd:{}", resultadoUnificadoGeral.size());
			watch.stop();
			
			// Separando os Grupos que se repetem 1540x
			List<GrupoPOJO> encontrados1540 = filtrarPorRepeticaoDeId(resultadoUnificadoGeral, 1540);
			log.info("Quantidade encontrados1540: {}", encontrados1540.size());
						
			// Exibe um resumo formatado no log
			log.info(watch.prettyPrint());
			log.info("Tempo total: " + watch.getTotalTimeSeconds() + " segundos.");
			
			exportarExcelParaDisco(encontrados1540);
			
			return encontrados1540;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		
	}
	
	public List<GrupoPOJO> gerarListaDeCombinacoes(HashMap<String, HashSet<HashSet<Integer>>> gruposComPositivosEncontrados, HashMap<String, HashSet<HashSet<Integer>>> gruposComOs9Encontrados) {
		List<GrupoPOJO> listaFinal = new ArrayList<>();

	    gruposComPositivosEncontrados.forEach((chave, setsDe6) -> {
	        
	        if (gruposComOs9Encontrados.containsKey(chave)) {
	            HashSet<HashSet<Integer>> setsDe9 = gruposComOs9Encontrados.get(chave);

	            for (HashSet<Integer> jogo6 : setsDe6) {
	                for (HashSet<Integer> jogo9 : setsDe9) {
	                    
	                    // União dos conjuntos
	                    //HashSet<Integer> uniao15 = new HashSet<>(jogo6);
	                    //uniao15.addAll(jogo9);
	                    // Altere de HashSet para LinkedHashSet para manter a ordem da união
	                    Set<Integer> uniao15 = new LinkedHashSet<>(jogo6); 
	                    uniao15.addAll(jogo9);
	                    
	                    // Criamos o POJO e adicionamos à lista
	                    // Isso é muito mais leve que criar um novo HashMap()
	                    listaFinal.add(new GrupoPOJO(chave, uniao15));
	                }
	            }
	        }
	    });

	    return listaFinal;
	}
	
	public void buscarGruposCompativeis(HashMap<String, HashSet<HashSet<Integer>>> gruposEncontrados, HashMap<String, HashSet<Integer>> gruposBase, List<Integer> numerosIdentificar) {

		// Como Grupos.grupos agora já é um HashSet, o containsAll é O(n) direto
		for (Map.Entry<String, HashSet<Integer>> entrada : gruposBase.entrySet()) {
						
			String idGrupo = entrada.getKey();
			
			HashSet<Integer> numerosDoGrupo = entrada.getValue();			

			// Verifica se o conjunto de 19 números possui TODOS os 6 da planilha
			if (numerosDoGrupo.containsAll(numerosIdentificar)) {		
				HashSet<Integer> numerosNovoGrupo = new LinkedHashSet<>(numerosIdentificar);
				
				if(gruposEncontrados.get(idGrupo) == null) {
					gruposEncontrados.put(idGrupo, new HashSet<HashSet<Integer>>());
				}
				gruposEncontrados.get(idGrupo).add(numerosNovoGrupo);
			}
		}		
	}
	
	/**
     * Remove os números negativos de uma sequência base e retorna os 9 restantes.
     * @param base A lista de 15 números original.
     * @param negativos A lista de 6 números a serem removidos.
     * @return List<Integer> contendo os 9 números resultantes.
     */
    public List<Integer> obterOs9Resultantes(List<Integer> baseDe15Numeros, List<Integer> negativos) {
        // 1. Usamos LinkedHashSet para manter a ordem original enquanto removemos
        Set<Integer> resultado = new LinkedHashSet<>(baseDe15Numeros);
        
        // 2. Remove todos os negativos da base de uma só vez
        resultado.removeAll(negativos);
        
        // 3. Converte de volta para lista para processamento posterior
        return new ArrayList<>(resultado);
    }
    
    public List<GrupoPOJO> filtrarPorRepeticaoDeId(List<GrupoPOJO> resultadoUnificadoGeral, int frequenciaAlvo) {
        
        // 1. Agrupamos pela String grupoId
        // O Mapa terá: Key = "G1 | C1", Value = Lista com todas as instâncias desse ID
        Map<String, List<GrupoPOJO>> agrupadoPorId = resultadoUnificadoGeral.stream()
                .collect(Collectors.groupingBy(GrupoPOJO::getGrupoId));

        // 2. Filtramos as listas que têm o tamanho desejado (ex: 1540)
        // E juntamos tudo em uma lista final
        return agrupadoPorId.values().stream()
                .filter(lista -> lista.size() == frequenciaAlvo)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
    public void exportarExcelParaDisco(List<GrupoPOJO> resultados) throws IOException {
    	log.info("Exportando o resultado parcial em um XLS.");
    	
    	// 1. Criar o sufixo de data e hora (Ex: 2026-02-20_18-24)
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
    	
        // IMPORTANTE: Use o caminho que você definiu à DIREITA do ":" no docker-compose
        String caminhoInternoContainer = "/app/temp"; 
        String nomeArquivo = "grupos_repetidos_1540x_"+timestamp+".xlsx";
        String caminhoCompleto = caminhoInternoContainer + "/" + nomeArquivo;
        log.info("Local do arquivo: {}", caminhoCompleto);

        // Garante que a pasta /app/temp exista dentro do container
        Files.createDirectories(Paths.get(caminhoInternoContainer));

        try (Workbook workbook = new XSSFWorkbook(); 
             FileOutputStream fileOut = new FileOutputStream(caminhoCompleto)) {
            
            Sheet sheet = workbook.createSheet("Grupos 1540");

            // Cabeçalho
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID Grupo");
            for (int i = 1; i <= 15; i++) {
                header.createCell(i).setCellValue("N" + i);
            }

            // Dados
            int rowIdx = 1;
            for (GrupoPOJO res : resultados) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(res.getGrupoId());

                // Ordenação para o Excel ficar profissional
                List<Integer> numerosOrdenados = new ArrayList<>(res.getNumeros());
                //Collections.sort(numerosOrdenados);

                int colIdx = 1;
                for (Integer num : numerosOrdenados) {
                    row.createCell(colIdx++).setCellValue(num);
                }
            }

            workbook.write(fileOut);
            System.out.println("✅ Arquivo gravado no container em " + caminhoCompleto + 
                               ". Verifique sua pasta C:/temp no Windows.");
        }
    }    
}

package br.com.srcsoftware.topomanager.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
			
			HashMap<String, HashSet<HashSet<Integer>>> gruposComPositivosEncontrados = new HashMap<>();
			//HashSet<HashMap<String, HashSet<Integer>>> gruposComPositivosEncontrados = new HashSet<>();
			//HashMap<String, HashSet<Integer>> gruposComPositivos = new HashMap<>();
			//ArrayList< HashMap<String, HashSet<Integer>> > gruposComPositivos = new ArrayList<>();

			watch.start("Separando todos os Grupos que possuam todos os Positivos.");
			log.info("Separando todos os Grupos que possuam todos os Positivos.");
			for(NumerosPositivos numeros: positivos) {
				buscarGruposCompativeis(gruposComPositivosEncontrados, Grupos.grupos, numeros.toArrayList());				
			}						
			log.info("Grupos encontrados com os POSITIVOS: Qtd:{}", gruposComPositivosEncontrados.size());
			watch.stop();
			
			
			HashSet<List<Integer>> resultado9Restantes = new HashSet<>();
			
			watch.start("Obtendo uma lista com 9 numeros.");
			//log.info("Obtendo uma lista com 9 numeros com base na remocao de todos os Negativos da Base de 15.");
			for(NumerosNegativos numeros: negativos) {
				//log.info("Analisando Negativos: {}", numeros);
				resultado9Restantes.add( obterOs9Resultantes(listaDos15Numeros, numeros.toArrayList()) );
				//log.info("9 Encontrados: {}", resultado9Restantes);				
			}	
			log.info("Os 9 gerados a partir dos 15: Qtd:{}", resultado9Restantes.size());
			watch.stop();
			
			HashMap<String, HashSet<HashSet<Integer>>> gruposComOs9Encontrados = new HashMap<>();
			//HashMap<String, HashSet<Integer>> gruposComOs9 = new HashMap<>();
			//HashSet<HashMap<String, HashSet<Integer>>> gruposComOs9Encontrados = new HashSet<>();
			
			//log.info("Separando todos os Grupos que possuam todos os 9.");
			//ArrayList< HashMap<String, HashSet<Integer>> > gruposComNegativos = new ArrayList<>();
			watch.start("Separando todos os Grupos que possuam todos os 9.");
			for(List<Integer> os9 : resultado9Restantes) {				
				buscarGruposCompativeis(gruposComOs9Encontrados, Grupos.grupos, os9);				
			}
			watch.stop();
						
			log.info("Grupos encontrados com os 9: Qtd:{}", gruposComOs9Encontrados.size());
				
			watch.start("Iniciando o processo de combinação dos Grupos");
			log.info("Iniciando o processo de combinação dos Grupos");
			List<GrupoPOJO> resultadoUnificadoGeral = gerarListaDeCombinacoes(gruposComPositivosEncontrados, gruposComOs9Encontrados);
			log.info("Finalizando o processo de combinação dos Grupos: Qtd:{}", resultadoUnificadoGeral.size());
			watch.stop();
			/*ArrayList<Map<String, HashSet<Integer>>> planilhas = new ArrayList<>();
			for(HashMap<String, HashSet<Integer>> encontradosPositivos : gruposComPositivosEncontrados) {
				for(HashMap<String, HashSet<Integer>> encontradosComOs9 : gruposComOs9Encontrados) {
					Map<String, HashSet<Integer>> gruposFinais = unificarResultados(encontradosComOs9, encontradosPositivos);
					planilhas.add(gruposFinais);
				}
			}
			log.info("Grupos Finalizados: Qtd:{}", planilhas.size());
*/
			
			// Exibe um resumo formatado no log
			log.info(watch.prettyPrint());
			log.info("Tempo total: " + watch.getTotalTimeSeconds() + " segundos.");
			
			return resultadoUnificadoGeral;
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
	                    HashSet<Integer> uniao15 = new HashSet<>(jogo6);
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
	
	// Este método consolida os 9 números negativos dentro do mapa que já tem os 6 positivos
	/*public Map<String, HashSet<Integer>> unificarResultados(HashMap<String, HashSet<Integer>> encontradosComOs9, HashMap<String, HashSet<Integer>> encontradosPositivos) {
		// 1. Criamos uma cópia profunda (Deep Copy) do primeiro mapa
	    // Assim, o 'encontradosPositivos' permanece intacto.
	    Map<String, HashSet<Integer>> mapaFinal = encontradosPositivos.entrySet().stream()
	        .collect(Collectors.toMap(
	            Map.Entry::getKey,
	            e -> new HashSet<>(e.getValue()) // Criamos um novo Set para não referenciar o mesmo objeto
	        ));

	    // 2. Mesclamos os dados do segundo mapa no mapaFinal
	    encontradosComOs9.forEach((key, novosNumeros) -> {
	        mapaFinal.computeIfPresent(key, (k, setExistente) -> {
	            setExistente.addAll(novosNumeros);
	            return setExistente;
	        });
	    });

	    return mapaFinal;
	}*/


	/**
	 * Identifica quais grupos contêm todos os números positivos da linha.
	 * @param positivosDaLinha Lista de 6 números (Colunas A a F)
	 * @return Lista com os grupos que deram match
	 */
	/*public HashMap<String, HashSet<Integer>> buscarGruposCompativeis(List<Integer> numerosIdentificar) {
		HashMap<String, HashSet<Integer>> gruposEncontrados = new HashMap<String, HashSet<Integer>>();

		// Como Grupos.grupos agora já é um HashSet, o containsAll é O(n) direto
		for (Map.Entry<String, java.util.HashSet<Integer>> entrada : Grupos.grupos.entrySet()) {
			String idGrupo = entrada.getKey();
			java.util.HashSet<Integer> numerosDoGrupo = entrada.getValue();

			// Verifica se o conjunto de 19 números possui TODOS os 6 da planilha
			if (numerosDoGrupo.containsAll(numerosIdentificar)) {
				gruposEncontrados.put(idGrupo, numerosDoGrupo);
			}
		}

		return gruposEncontrados;
	}*/
	
	/** Comentado para backup
	 * Identifica quais grupos contêm todos os números positivos da linha.
	 * @param HashMap<String, HashSet<Integer>> gruposBase - Grupos base.
	 * @param List<Integer> numerosIdentificar - Lista de números que devem dar Match.
	 * @return Lista com os grupos que deram match contendo apenas os numeros que foram identificados.
	 */
	/*public HashMap<String, HashSet<Integer>> buscarGruposCompativeis(HashMap<String, HashSet<Integer>> gruposBase, List<Integer> numerosIdentificar) {
		HashMap<String, HashSet<Integer>> gruposEncontrados = new HashMap<String, HashSet<Integer>>();

		// Como Grupos.grupos agora já é um HashSet, o containsAll é O(n) direto
		for (Map.Entry<String, HashSet<Integer>> entrada : gruposBase.entrySet()) {
						
			String idGrupo = entrada.getKey();
			
			HashSet<Integer> numerosDoGrupo = entrada.getValue();			

			// Verifica se o conjunto de 19 números possui TODOS os 6 da planilha
			if (numerosDoGrupo.containsAll(numerosIdentificar)) {		
				HashSet<Integer> numerosNovoGrupo = new LinkedHashSet<>(numerosIdentificar);
				gruposEncontrados.put(idGrupo, numerosNovoGrupo);				
			}
		}
		return gruposEncontrados;
	}*/
	
	//HashMap<String, HashSet<HashSet<Integer>>> gruposEncontrados = new HashMap<String, HashSet<HashSet<Integer>>>();
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
}

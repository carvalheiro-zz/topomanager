package br.com.srcsoftware.topomanager.service;

import java.io.BufferedWriter;
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
import java.util.LinkedHashSet; // Importado para manter a ordem
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import br.com.srcsoftware.topomanager.constantes.Grupos;
import br.com.srcsoftware.topomanager.constantes.NumerosBase;
import br.com.srcsoftware.topomanager.model.po.NumerosNegativos;
import br.com.srcsoftware.topomanager.model.po.NumerosPositivos;
import br.com.srcsoftware.topomanager.model.pojo.GrupoPOJO;
import br.com.srcsoftware.topomanager.model.pojo.GruposAgrupadosPOJO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentificadorService {

	private final ExcelService excelService;
	
	public List<GrupoPOJO> processar(InputStream is, List<Integer> listaDos15Numeros) throws Exception{
		try {
			StopWatch watch = new StopWatch("Processamento TopoManager");
			
			List<GrupoPOJO> resultadoCenario1 = processarCenario1(is, listaDos15Numeros);
			
			ArrayList<GruposAgrupadosPOJO> gruposSemOs6 = processarCenario2(watch, resultadoCenario1);
			
			try {
				exportarGruposAgrupadosParaTxtParaCadaGrupo("4_Iguais_Sem_Os_6",gruposSemOs6);
			} catch (IOException e) {

				e.printStackTrace();
			}
			
			return resultadoCenario1;
		} catch (Exception e) {
			log.error("Problema ao processar.",e);
			throw e;
		}
	}

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
			for (NumerosPositivos numeros : positivos) {
				buscarGruposCompativeis(gruposComPositivosEncontrados, Grupos.grupos, numeros.toLinkedHashSet());
			}
			watch.stop();

			// Mantendo a ordem de remoção com LinkedHashSet
			LinkedHashSet<LinkedHashSet<Integer>> resultado9Restantes = new LinkedHashSet<>();

			watch.start("Obtendo uma lista com 9 numeros.");
			for (NumerosNegativos numeros : negativos) {
				resultado9Restantes.add(obterOs9Resultantes(listaDos15Numeros, numeros.toArrayList()));
			}
			watch.stop();

			Map<String, LinkedHashSet<LinkedHashSet<Integer>>> gruposComOs9Encontrados = new HashMap<>();

			watch.start("Separando todos os Grupos que possuam todos os 9.");
			for (LinkedHashSet<Integer> os9 : resultado9Restantes) {
				buscarGruposCompativeis(gruposComOs9Encontrados, Grupos.grupos, os9);
			}
			watch.stop();

			watch.start("Iniciando o processo de combinação dos Grupos");
			List<GrupoPOJO> resultadoUnificadoGeral = gerarListaDeCombinacoes(gruposComPositivosEncontrados, gruposComOs9Encontrados);
			watch.stop();

			List<GrupoPOJO> encontrados1540 = filtrarPorRepeticaoDeId(resultadoUnificadoGeral, 1540);

			// Novo filtro aplicado sobre o resultado anterior
			List<GrupoPOJO> listaFinalComFiltroSetimaPosicao = filtrarPorDiversidadeNaSetimaPosicao(encontrados1540);

			log.info("Quantidade após filtro de diversidade na 7ª posição: {}", listaFinalComFiltroSetimaPosicao.size());

			log.info(watch.prettyPrint());
			log.info("Tempo total: {} segundos.", watch.getTotalTimeSeconds());

			return listaFinalComFiltroSetimaPosicao;
		} catch (Exception e) {
			log.error("Erro no processamento do Cenário 1", e);
			throw e;
		}
	}

	public List<GrupoPOJO> gerarListaDeCombinacoes(Map<String, LinkedHashSet<LinkedHashSet<Integer>>> gruposComPositivosEncontrados, Map<String, LinkedHashSet<LinkedHashSet<Integer>>> gruposComOs9Encontrados) {

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

	public void buscarGruposCompativeis(Map<String, LinkedHashSet<LinkedHashSet<Integer>>> gruposEncontrados, Map<String, LinkedHashSet<Integer>> gruposBase, LinkedHashSet<Integer> numerosIdentificar) {

		for (Map.Entry<String, LinkedHashSet<Integer>> entrada : gruposBase.entrySet()) {
			String idGrupo = entrada.getKey();
			LinkedHashSet<Integer> numerosDoGrupo = entrada.getValue();

			if (numerosDoGrupo.containsAll(numerosIdentificar)) {
				LinkedHashSet<Integer> numerosNovoGrupo = new LinkedHashSet<>(numerosIdentificar);

				// Uso de computeIfAbsent para simplificar e garantir LinkedHashSet
				gruposEncontrados.computeIfAbsent(idGrupo, k -> new LinkedHashSet<>()).add(numerosNovoGrupo);
			}
		}
	}

	public LinkedHashSet<Integer> obterOs9Resultantes(List<Integer> baseDe15Numeros, List<Integer> negativos) {
		Set<Integer> resultado = new LinkedHashSet<>(baseDe15Numeros);
		resultado.removeAll(negativos);
		return new LinkedHashSet<>(resultado);
	}

	public List<GrupoPOJO> filtrarPorRepeticaoDeId(List<GrupoPOJO> resultadoUnificadoGeral, int frequenciaAlvo) {
		Map<String, List<GrupoPOJO>> agrupadoPorId = resultadoUnificadoGeral.stream().collect(Collectors.groupingBy(GrupoPOJO::getGrupoId));

		return agrupadoPorId.values().stream().filter(lista -> lista.size() == frequenciaAlvo).flatMap(List::stream).collect(Collectors.toList());
	}

	public String exportarExcelParaDisco(List<GrupoPOJO> resultados) throws IOException {
		log.info("Exportando o resultado parcial em um XLS.");

		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
		String caminhoInternoContainer = "/app/temp";
		String nomeArquivo = "grupos_repetidos_1540x_" + timestamp + ".xlsx";
		String caminhoCompleto = caminhoInternoContainer + "/" + nomeArquivo;

		Files.createDirectories(Paths.get(caminhoInternoContainer));

		try (Workbook workbook = new XSSFWorkbook(); FileOutputStream fileOut = new FileOutputStream(caminhoCompleto)) {

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

	public List<GrupoPOJO> filtrarPorDiversidadeNaSetimaPosicao(List<GrupoPOJO> listaOriginal) {
		// 1. Agrupamos todos os objetos pelo grupoId
		Map<String, List<GrupoPOJO>> agrupadoPorId = listaOriginal.stream().collect(Collectors.groupingBy(GrupoPOJO::getGrupoId));

		// 2. Filtramos os grupos que atendem à regra dos "apenas 3 diferentes"
		return agrupadoPorId.values().stream().filter(listaDoGrupo -> {
			// Coletamos todos os números que aparecem na 7ª posição (índice 6) deste ID
			// específico
			Set<Integer> numerosDistintosNaPosicao7 = listaDoGrupo.stream().map(grupo -> {
				// Convertemos o LinkedHashSet para List para acessar o índice 6 com segurança
				List<Integer> temp = new ArrayList<>(grupo.getNumeros());
				return temp.get(6);
			}).collect(Collectors.toSet()); // O Set elimina duplicatas automaticamente

			// A regra: Só queremos se houver EXATAMENTE (ou no máximo) 3 números diferentes
			return numerosDistintosNaPosicao7.size() == 3;
		})
				// 3. Transformamos as listas aprovadas de volta em uma lista única de objetos
				.flatMap(List::stream).collect(Collectors.toList());
	}

	public Map<String, LinkedHashSet<LinkedHashSet<Integer>>> transformarParaMapEspecial(List<GrupoPOJO> lista) {
		return lista.stream().collect(Collectors.groupingBy(GrupoPOJO::getGrupoId, HashMap::new, Collectors.mapping(grupo -> (LinkedHashSet<Integer>) grupo.getNumeros(), Collectors.toCollection(LinkedHashSet::new))));
	}

	public ArrayList<GruposAgrupadosPOJO> processarCenario2(StopWatch watch, List<GrupoPOJO> resultados1540) throws IOException {
		log.info("Separando todos os Grupos com quantidades encontradas de 28 e 55, que não possuam os 2 numeros.");

		watch.start("Separando todos os Grupos com quantidades encontradas de 28 e 55, que não possuam os 2 numeros.");

		List<GruposAgrupadosPOJO> gruposAgrupados = agruparGrupos(resultados1540);

		ArrayList<GruposAgrupadosPOJO> gruposSemOsParesCom28e55 = obterGruposSemOsParesCom28e55(gruposAgrupados, NumerosBase.negativosDe2);
		log.info("Gupos encontrados: {}", gruposSemOsParesCom28e55.size());
		
		// Separando todos os grupos encontrados numa lista para os de 28 e outra para
		// os outros de 55
		log.info("Separando todos os grupos encontrados numa lista para os de 28 e outra para os outros de 55.");
		Map<String, List<GruposAgrupadosPOJO>> gruposCom28 = reagruparGruposSemOsPares_Com28e55(28, gruposSemOsParesCom28e55);// FIXME Exportar toda essa lista de 28. Uma pasta para cada Grupo com os 28.
		Map<String, List<GruposAgrupadosPOJO>> gruposCom55 = reagruparGruposSemOsPares_Com28e55(55, gruposSemOsParesCom28e55);
					
		// Achata (flatten) todos os Lists do Map em uma única List<GruposAgrupadosPOJO>
		List<GruposAgrupadosPOJO> listaConsolidada = gruposCom28.values().stream()
		    .flatMap(List::stream)
		    .collect(Collectors.toList());

		// Chama o método uma única vez
		exportarGruposAgrupadosParaTxtParaCadaGrupo("28_sem_os_2",listaConsolidada);

		log.info("Separando apenas 1 de 28 de cada grupo.");
		List<GruposAgrupadosPOJO> separados1de28 = separarApenasUmDeCadaPorGrupo(gruposCom28);

		// Pegando os 6 primeiro numeros
		List<GruposAgrupadosPOJO> os6PrimeirosNumeros = new ArrayList<>();
		for (GruposAgrupadosPOJO grupoPOJO : separados1de28) {
			//log.info("Gupo de 28 {}: {}", grupoPOJO.getGrupoId(), grupoPOJO.getAgrupamentosDeNumeros().size());

			GruposAgrupadosPOJO grupoUnicaRepeticao = extrairApenasOs6Primeiros(grupoPOJO);
			os6PrimeirosNumeros.add(grupoUnicaRepeticao);
		}
		log.info("Os 6 Primeiros numeros dos grupos de 28: {}", os6PrimeirosNumeros);

		log.info("Separando apenas 1 de 55 de cada grupo.");
		List<GruposAgrupadosPOJO> separados1de55 = separarApenasUmDeCadaPorGrupo(gruposCom55);

		// Pegando a linha cujo numero na posicao 7 nunca se repete
		List<GruposAgrupadosPOJO> os9UltimosDaLinhaQueNaoSeRepete = new ArrayList<>();
		for (GruposAgrupadosPOJO grupoPOJO : separados1de55) {
			//log.info("Gupo de 55 {}: {}", grupoPOJO.getGrupoId(), grupoPOJO.getAgrupamentosDeNumeros().size());
			GruposAgrupadosPOJO grupoUnicaRepeticao = extrairUltimos9DaCadeiaUnica(grupoPOJO);
			os9UltimosDaLinhaQueNaoSeRepete.add(grupoUnicaRepeticao);
		}
		log.info("Os 9 Ultimos numeros dos grupos de 55: {}", os9UltimosDaLinhaQueNaoSeRepete);

		log.info("Unindo os 9 em todos os 6 de cada Grupo.");
		List<GruposAgrupadosPOJO> os15 = unirOs6PrimeirosCom9Ultimos(os6PrimeirosNumeros, os9UltimosDaLinhaQueNaoSeRepete);
		log.info("Os 15 gerados: {}.", os15);

		log.info("Obter os grupos que não possuem os 6");

		ArrayList<GruposAgrupadosPOJO> gruposSemOs6 = obterGruposSemOsParesDe6(os15, NumerosBase.negativosDe6);
		//gruposSemOs6.forEach(grupo -> log.info("{} Grupos obtidos: {}", grupo.getAgrupamentosDeNumeros().size(), grupo));

		List<GruposAgrupadosPOJO> gruposSemOs6ComOs4PrimeirosIguais4Vezes = filtrarPorSequenciaRepetida(gruposSemOs6);
		
		watch.stop();
		
		return (ArrayList<GruposAgrupadosPOJO>) gruposSemOs6ComOs4PrimeirosIguais4Vezes;
	}

	private List<GruposAgrupadosPOJO> unirOs6PrimeirosCom9Ultimos(List<GruposAgrupadosPOJO> os6PrimeirosNumeros, List<GruposAgrupadosPOJO> os9UltimosDaLinhaQueNaoSeRepete) {
		List<GruposAgrupadosPOJO> os15 = new ArrayList<>();

		for (GruposAgrupadosPOJO os6 : os6PrimeirosNumeros) {
			GruposAgrupadosPOJO agrupadosPOJO = new GruposAgrupadosPOJO();
			agrupadosPOJO.setGrupoId(os6.getGrupoId());
			if (agrupadosPOJO.getAgrupamentosDeNumeros() == null) {
				agrupadosPOJO.setAgrupamentosDeNumeros(new LinkedHashSet<>());
			}
			agrupadosPOJO.getAgrupamentosDeNumeros().addAll(os6.getAgrupamentosDeNumeros());

			for (GruposAgrupadosPOJO os9 : os9UltimosDaLinhaQueNaoSeRepete) {
				if (os9.getGrupoId().equalsIgnoreCase(agrupadosPOJO.getGrupoId())) {

					for (LinkedHashSet<Integer> cadeiaDe6 : agrupadosPOJO.getAgrupamentosDeNumeros()) {
						for (LinkedHashSet<Integer> cadeiaDe9 : os9.getAgrupamentosDeNumeros()) {
							cadeiaDe6.addAll(cadeiaDe9);
						}
					}
				}
			}

			os15.add(agrupadosPOJO);
		}
		return os15;
	}

	public GruposAgrupadosPOJO extrairApenasOs6Primeiros(GruposAgrupadosPOJO grupoOriginal) {
		// Criamos um novo LinkedHashSet para as cadeias reduzidas
		LinkedHashSet<LinkedHashSet<Integer>> cadeiasReduzidas = grupoOriginal.getAgrupamentosDeNumeros().stream().map(cadeia -> {
			// Slicing: Pega exatamente do índice 0 ao 5
			return cadeia.stream().limit(6).collect(Collectors.toCollection(LinkedHashSet::new));
		}).collect(Collectors.toCollection(LinkedHashSet::new));

		// Retorna o novo objeto mantendo a identidade do Grupo
		GruposAgrupadosPOJO resultado = new GruposAgrupadosPOJO();
		resultado.setGrupoId(grupoOriginal.getGrupoId());
		resultado.setAgrupamentosDeNumeros(cadeiasReduzidas);

		return resultado;
	}

	public GruposAgrupadosPOJO extrairUltimos9DaCadeiaUnica(GruposAgrupadosPOJO grupo) {
		// 1. Mapeamos a frequência da 7ª posição (índice 6)
		Map<Integer, Long> frequencias = grupo.getAgrupamentosDeNumeros().stream().map(cadeia -> {
			List<Integer> lista = new ArrayList<>(cadeia);
			return lista.size() >= 7 ? lista.get(6) : null;
		}).filter(Objects::nonNull).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

		// 2. Localizamos o "Número Mágico" (Frequência == 1)
		Integer numeroMagico = frequencias.entrySet().stream().filter(entry -> entry.getValue() == 1).map(Map.Entry::getKey).findFirst().orElse(null);

		if (numeroMagico != null) {
			// 3. Localizamos a cadeia original
			LinkedHashSet<Integer> cadeiaOriginal = grupo.getAgrupamentosDeNumeros().stream().filter(cadeia -> {
				List<Integer> lista = new ArrayList<>(cadeia);
				return lista.size() >= 7 && lista.get(6).equals(numeroMagico);
			}).findFirst().orElse(null);

			if (cadeiaOriginal != null) {
				// 4. Transformamos para pegar apenas os últimos 9 números
				int totalElementos = cadeiaOriginal.size();
				LinkedHashSet<Integer> ultimos9 = cadeiaOriginal.stream().skip(Math.max(0, totalElementos - 9)) // Pula tudo até sobrarem os últimos 9
						.collect(Collectors.toCollection(LinkedHashSet::new));

				GruposAgrupadosPOJO resultado = new GruposAgrupadosPOJO();
				resultado.setGrupoId(grupo.getGrupoId());

				LinkedHashSet<LinkedHashSet<Integer>> container = new LinkedHashSet<>();
				container.add(ultimos9);
				resultado.setAgrupamentosDeNumeros(container);

				return resultado;
			}
		}
		return null;
	}
	
	private Map<String, List<GruposAgrupadosPOJO>> reagruparGruposSemOsPares_Com28e55(int separar28Ou55, List<GruposAgrupadosPOJO> gruposSemOsParesCom28e55) {

		return gruposSemOsParesCom28e55.stream()
				// 1. Filtro de volume (Early Return funcional)
				.filter(g -> g.getAgrupamentosDeNumeros().size() == separar28Ou55)
				//.peek(g -> log.info("Grupo {} {}: {}", extrairIdBase(g.getGrupoId()), g.getGrupoId(), g.getAgrupamentosDeNumeros().size()))
				// 2. Agrupamento eficiente
				.collect(Collectors.groupingBy(g -> extrairIdBase(g.getGrupoId()), HashMap::new, Collectors.toList()));
	}

	/**
	 * Extrai o ID base antes do hífen de forma mais performática que split().
	 */
	private String extrairIdBase(String fullId) {
		int index = fullId.indexOf('-');
		return (index == -1) ? fullId : fullId.substring(0, index);
	}
	
	private List<GruposAgrupadosPOJO> separarApenasUmDeCadaPorGrupo(Map<String, List<GruposAgrupadosPOJO>> gruposCom28ou55) {
		// Separando apenas uma cadeia de numeros (28 ou 55) para cada grupo encontrado
		List<GruposAgrupadosPOJO> retorno = new ArrayList<>();
		for (String key : gruposCom28ou55.keySet()) {
			GruposAgrupadosPOJO novoGrupoComUmDe28UmDe55 = new GruposAgrupadosPOJO();
			novoGrupoComUmDe28UmDe55.setGrupoId(key);
			novoGrupoComUmDe28UmDe55.setAgrupamentosDeNumeros(new LinkedHashSet<>());

			// Percorrendo a lista de Grupos com os 2 - Ex: G4|C9-[1, 4]
			List<GruposAgrupadosPOJO> guposSeparados = gruposCom28ou55.get(key);
			for (GruposAgrupadosPOJO grupoCorrente : guposSeparados) {
				novoGrupoComUmDe28UmDe55.getAgrupamentosDeNumeros().addAll(grupoCorrente.getAgrupamentosDeNumeros());
				break;
			}
			retorno.add(novoGrupoComUmDe28UmDe55);
		}
		return retorno;
	}
	
	public ArrayList<GruposAgrupadosPOJO> obterGruposSemOsParesDe2(List<GruposAgrupadosPOJO> listaOriginal, List<LinkedHashSet<Integer>> numerosDe2) {

		ArrayList<GruposAgrupadosPOJO> gruposAgrupados = new ArrayList<>();

		for (GruposAgrupadosPOJO agrupado : listaOriginal) {
			
			for (LinkedHashSet<Integer> numerosNaoDeveTer : numerosDe2) {
				
				GruposAgrupadosPOJO agrupamentos = new GruposAgrupadosPOJO();

				for (LinkedHashSet<Integer> numerosGrupo : agrupado.getAgrupamentosDeNumeros()) {
					
					// Se retornar true, o grupo está "limpo" (não tem nenhum dos números proibidos)
					boolean estaLimpo = Collections.disjoint(numerosGrupo, numerosNaoDeveTer);
					if (estaLimpo) {
						
						if (agrupamentos.getAgrupamentosDeNumeros() == null) {
							agrupamentos.setAgrupamentosDeNumeros(new LinkedHashSet<>());
						}
						agrupamentos.getAgrupamentosDeNumeros().add(numerosGrupo);
					} 
				}

				int quantidadeLinhasEncontradas = agrupamentos.getAgrupamentosDeNumeros().size();
				if (quantidadeLinhasEncontradas != 28 && quantidadeLinhasEncontradas != 55) {
					continue;
				}

				agrupamentos.setGrupoId(agrupado.getGrupoId() + "-" + numerosNaoDeveTer.toString() + " " + agrupamentos.getAgrupamentosDeNumeros().size());
				//log.info("{} {} não contem {}", agrupamentos.getGrupoId(), numerosNaoDeveTer.toString(), agrupamentos.getAgrupamentosDeNumeros().size());
				gruposAgrupados.add(agrupamentos);
			}
		}
		
		return gruposAgrupados;		
	}

	public ArrayList<GruposAgrupadosPOJO> obterGruposSemOsParesCom28e55(List<GruposAgrupadosPOJO> listaOriginal, List<LinkedHashSet<Integer>> numerosDe2) {

		ArrayList<GruposAgrupadosPOJO> gruposAgrupados = new ArrayList<>();

		for (LinkedHashSet<Integer> numerosNaoDeveTer : numerosDe2) {
			
			for (GruposAgrupadosPOJO agrupado : listaOriginal) {				

				GruposAgrupadosPOJO agrupamentos = new GruposAgrupadosPOJO();
				agrupamentos.setGrupoId(agrupado.getGrupoId() + "-" + numerosNaoDeveTer.toString());
				
				for (LinkedHashSet<Integer> numerosGrupo : agrupado.getAgrupamentosDeNumeros()) {

					// if(!numerosGrupo.containsAll(numerosNaoDeveTer)) {
					// Se retornar true, o grupo está "limpo" (não tem nenhum dos números proibidos)
					boolean estaLimpo = Collections.disjoint(numerosGrupo, numerosNaoDeveTer);
					if (estaLimpo) {
						
						if (agrupamentos.getAgrupamentosDeNumeros() == null) {
							agrupamentos.setAgrupamentosDeNumeros(new LinkedHashSet<>());
						}
						agrupamentos.getAgrupamentosDeNumeros().add(numerosGrupo);
					} 
				}

				int quantidadeLinhasEncontradas = agrupamentos.getAgrupamentosDeNumeros().size();
				if (quantidadeLinhasEncontradas != 28 && quantidadeLinhasEncontradas != 55) {
					continue;
				}

				gruposAgrupados.add(agrupamentos);
			}
		}
		
		return gruposAgrupados;
	}

	public List<GruposAgrupadosPOJO> agruparGrupos(List<GrupoPOJO> listaOriginal) {
		// 1. Primeiro agrupamos em um Map intermediário para organizar por ID
		Map<String, LinkedHashSet<LinkedHashSet<Integer>>> mapaIntermediario = listaOriginal.stream()
				.collect(Collectors.groupingBy(GrupoPOJO::getGrupoId, HashMap::new, Collectors.mapping(grupo -> (LinkedHashSet<Integer>) grupo.getNumeros(), Collectors.toCollection(LinkedHashSet::new))));

		// 2. Convertemos o Map para a sua lista de GruposAgrupadosPOJO
		return mapaIntermediario.entrySet().stream().map(entry -> {
			GruposAgrupadosPOJO agrupado = new GruposAgrupadosPOJO();
			agrupado.setGrupoId(entry.getKey());
			agrupado.setAgrupamentosDeNumeros(entry.getValue());
			return agrupado;
		}).collect(Collectors.toList());
	}
	
	public void exportarGruposAgrupadosParaTxtParaCadaGrupo(String tipoLista, List<GruposAgrupadosPOJO> listaAgrupada) throws IOException {
		log.info("Iniciando exportação para arquivo TXT.");

		String pasta = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))+"-"+tipoLista;
				

		for (GruposAgrupadosPOJO item : listaAgrupada) {
			String id = item.getGrupoId();
			
			String nomeGrupo = id.split("-")[0].trim();
			String caminhoInternoContainer = "/app/temp/"+pasta+"/"+nomeGrupo.replace("|", "");
			
			
			String nomeArquivo = id.replace("|", "") + ".txt";
			String caminhoCompleto = caminhoInternoContainer + "/" + nomeArquivo;

			// Garante que o diretório existe
			Files.createDirectories(Paths.get(caminhoInternoContainer));
			
			try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(caminhoCompleto))) {

				// Percorremos cada LinkedHashSet<Integer> dentro do LinkedHashSet principal
				for (LinkedHashSet<Integer> jogo : item.getAgrupamentosDeNumeros()) {
					// StringBuilder para construir a linha com espaços
					StringBuilder linha = new StringBuilder();

					// 2. Adiciona cada número do jogo separado por espaço
					for (Integer num : jogo) {
						linha.append(" ").append(num);
					}

					// 1. Adiciona o grupoId
					linha.append(" ").append(nomeGrupo.split("-")[0].trim());

					// Escreve a linha no arquivo e pula para a próxima
					writer.write(linha.toString());
					writer.newLine();
				}
				log.info("✅ Arquivo TXT gravado com sucesso em: {}", caminhoCompleto);
			} catch (IOException e) {
				log.error("Erro ao gravar arquivo TXT", e);
				throw e;
			}
		}
	}

	public ArrayList<GruposAgrupadosPOJO> obterGruposSemOs6(List<GruposAgrupadosPOJO> listaOriginal, List<LinkedHashSet<Integer>> numerosDe2) {

		ArrayList<GruposAgrupadosPOJO> gruposAgrupados = new ArrayList<>();

		for (LinkedHashSet<Integer> numerosNaoDeveTer : numerosDe2) {
			for (GruposAgrupadosPOJO agrupado : listaOriginal) {

				GruposAgrupadosPOJO agrupamentos = new GruposAgrupadosPOJO();
				agrupamentos.setGrupoId(agrupado.getGrupoId());

				for (LinkedHashSet<Integer> numerosGrupo : agrupado.getAgrupamentosDeNumeros()) {

					// if(!numerosGrupo.containsAll(numerosNaoDeveTer)) {
					// Se retornar true, o grupo está "limpo" (não tem nenhum dos números proibidos)
					boolean contemTodos = numerosGrupo.containsAll(numerosNaoDeveTer);
					if (!contemTodos) {

						if (agrupamentos.getAgrupamentosDeNumeros() == null) {
							agrupamentos.setAgrupamentosDeNumeros(new LinkedHashSet<>());
						}
						agrupamentos.getAgrupamentosDeNumeros().add(numerosGrupo);
					} 
				}

				//log.info("{} {} {}", agrupamentos.getAgrupamentosDeNumeros().size(), agrupado.getGrupoId(), numerosNaoDeveTer.toString());
				gruposAgrupados.add(agrupamentos);
			}
		}

		return gruposAgrupados;
	}

	public ArrayList<GruposAgrupadosPOJO> obterGruposSemOsParesDe6(List<GruposAgrupadosPOJO> listaOriginal, List<LinkedHashSet<Integer>> numerosDe2) {
		
		ArrayList<GruposAgrupadosPOJO> gruposAgrupados = new ArrayList<>();

		for (GruposAgrupadosPOJO agrupado : listaOriginal) {
			
			//log.info("Grupo com {} {}", agrupado.getGrupoId(), agrupado.getAgrupamentosDeNumeros().size());

			for (LinkedHashSet<Integer> numerosNaoDeveTer : numerosDe2) {
				
				GruposAgrupadosPOJO agrupamentos = new GruposAgrupadosPOJO();

				for (LinkedHashSet<Integer> numerosGrupo : agrupado.getAgrupamentosDeNumeros()) {

					// Se retornar true, o grupo está "limpo" (não tem nenhum dos números proibidos)
					boolean naoContemNenhum = Collections.disjoint(numerosGrupo, numerosNaoDeveTer);
					if (naoContemNenhum) {

						if (agrupamentos.getAgrupamentosDeNumeros() == null) {
							agrupamentos.setAgrupamentosDeNumeros(new LinkedHashSet<>());
						}
						agrupamentos.getAgrupamentosDeNumeros().add(numerosGrupo);
					} 
				}

				if (agrupamentos.getAgrupamentosDeNumeros() == null || agrupamentos.getAgrupamentosDeNumeros().size() == 0) {
					continue;
				}
				agrupamentos.setGrupoId(agrupado.getGrupoId() + " - " + numerosNaoDeveTer.toString().replace("[", "").replace("]", "") + " - " + agrupamentos.getAgrupamentosDeNumeros().size());
				//log.info("Não contem {}", agrupamentos.getGrupoId());
				gruposAgrupados.add(agrupamentos);
			}
		}
		
		return gruposAgrupados;

	}
	
	public List<GruposAgrupadosPOJO> filtrarPorSequenciaRepetida(List<GruposAgrupadosPOJO> gruposSemOs6) {
	    
	    // 1. Agrupamos os elementos pelos 4 primeiros números do grupoId
	    Map<String, List<GruposAgrupadosPOJO>> gruposPorAssinatura = gruposSemOs6.stream()
	        .collect(Collectors.groupingBy(pojo -> extrairAssinaturaCompleta(pojo.getGrupoId())));

	    // 2. Filtramos o mapa para manter apenas onde a lista tem 4 ou mais elementos
	    // e achatamos (flatMap) de volta para uma lista única
	    return gruposPorAssinatura.values().stream()
	        .filter(lista -> lista.size() == 4) // Use >= 4 se puder haver mais de 4
	        .flatMap(List::stream)
	        .collect(Collectors.toList());
	}
	
	private String extrairAssinaturaCompleta(String grupoId) {
	    if (grupoId == null || !grupoId.contains("-")) return "";
	    
	    // 1. Separa o prefixo (ex: G4C9) da parte numérica
	    String[] partes = grupoId.split("-");
	    String prefixo = partes[0].trim(); // Pega "G4C9"
	    String numerosParte = partes[1].trim(); // Pega "8, 10, 14, 25, 17, 18"
	    
	    // 2. Pega os 4 primeiros números
	    String[] tokens = numerosParte.split(",\\s*");
	    if (tokens.length >= 4) {
	        // A assinatura agora é "G4C9|8,10,14,25"
	        return prefixo + "|" + String.join(",", tokens[0], tokens[1], tokens[2], tokens[3]);
	    }
	    
	    return grupoId; // Fallback caso a string seja curta
	}
}

package br.com.srcsoftware.topomanager.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.srcsoftware.topomanager.model.po.RegistroExcel;
import br.com.srcsoftware.topomanager.repository.RegistroRepository;

@ExtendWith(MockitoExtension.class)
class ExportarExcelServiceTest {

    @Mock
    private RegistroRepository repository;

    @InjectMocks
    private ExportarExcelService exportarExcelService;

    @Test
    void deveGerarArquivoExcelComDados() throws Exception {
        // Mock dos dados retornados pela query
        List<RegistroExcel> mockDados = List.of(
                RegistroExcel.builder().id(1L).colunaA(2).colunaB(10).colunaC("Teste").build()
        );

        when(repository.findFiltroCustomizado()).thenReturn(mockDados);

        // Execução
        byte[] excelBytes = exportarExcelService.gerarRelatorioExcel();

        // Verificação
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0, "O arquivo gerado não deve estar vazio");
    }
}
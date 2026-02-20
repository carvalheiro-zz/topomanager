package br.com.srcsoftware.topomanager.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import br.com.srcsoftware.topomanager.model.po.RegistroExcel;

@DataJpaTest
@ActiveProfiles("test") // Usa o application-test.properties se existir
class RegistroRepositoryTest {

    @Autowired
    private RegistroRepository repository;

    @Test
    @DisplayName("Deve retornar apenas registros onde A é par e B é múltiplo de 10")
    void deveFiltrarRegistrosCorretamente() {
        // Cenário (Given)
        RegistroExcel r1 = RegistroExcel.builder().colunaA(2).colunaB(20).build(); // Válido
        RegistroExcel r2 = RegistroExcel.builder().colunaA(3).colunaB(20).build(); // Ímpar (Inválido)
        RegistroExcel r3 = RegistroExcel.builder().colunaA(4).colunaB(15).build(); // Não múltiplo de 10 (Inválido)
        RegistroExcel r4 = RegistroExcel.builder().colunaA(10).colunaB(100).build(); // Válido

        repository.saveAll(List.of(r1, r2, r3, r4));

        // Execução (When)
        List<RegistroExcel> resultado = repository.findFiltroCustomizado();

        // Verificação (Then)
        assertEquals(2, resultado.size(), "Deveria encontrar exatamente 2 registros válidos");
        
        assertTrue(resultado.stream().allMatch(r -> r.getColunaA() % 2 == 0), "Todos os A devem ser pares");
        assertTrue(resultado.stream().allMatch(r -> r.getColunaB() % 10 == 0), "Todos os B devem ser múltiplos de 10");
        
        // Verifica se os valores específicos estão lá
        assertTrue(resultado.stream().anyMatch(r -> r.getColunaA() == 2 && r.getColunaB() == 20));
        assertTrue(resultado.stream().anyMatch(r -> r.getColunaA() == 10 && r.getColunaB() == 100));
    }
}
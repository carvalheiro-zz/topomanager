package br.com.srcsoftware.topomanager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import br.com.srcsoftware.topomanager.model.po.RegistroExcel;

@Repository
public interface RegistroRepository extends JpaRepository<RegistroExcel, Long> {
	
	// Filtra: Coluna A é par (MOD 2 = 0) e Coluna B é múltiplo de 10 (MOD 10 = 0)
    @Query("SELECT r FROM RegistroExcel r WHERE MOD(r.colunaA, 2) = 0 AND MOD(r.colunaB, 413) = 0")
    List<RegistroExcel> findFiltroCustomizado();
    
}

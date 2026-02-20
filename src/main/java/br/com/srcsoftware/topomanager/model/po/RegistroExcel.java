package br.com.srcsoftware.topomanager.model.po;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dados_planilha")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegistroExcel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer colunaA;
    private Integer colunaB;
    private String colunaC;
    private String colunaD;
    private String colunaE;
    private String colunaF;
    private String colunaG;
}


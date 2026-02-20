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
public class GruposDe19PO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String grupo;
    private Integer valor1;
    private Integer valor2;
    private Integer valor3;
    private Integer valor4;
    private Integer valor5;
    private Integer valor6;
    private Integer valor7;
    private Integer valor8;
    private Integer valor9;
    private Integer valor10;
    private Integer valor11;
    private Integer valor12;
    private Integer valor13;
    private Integer valor14;
    private Integer valor15;
    private Integer valor16;
    private Integer valor17;
    private Integer valor18;
    private Integer valor19;
}


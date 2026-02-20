package br.com.srcsoftware.topomanager.model.po;

import java.util.ArrayList;
import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NumerosPositivos extends Numeros {

	private Integer colunaA;
	private Integer colunaB;
	private Integer colunaC;
	private Integer colunaD;
	private Integer colunaE;
	private Integer colunaF;
	
	/**
     * Retorna os atributos da classe como um ArrayList de Integer.
     * Ideal para ser usado em comparacoes com colecoes (ex: containsAll).
     */
    public ArrayList<Integer> toArrayList() {
        // Criamos uma lista a partir dos atributos e passamos para o construtor do ArrayList
        return new ArrayList<>(Arrays.asList(
            this.colunaA, 
            this.colunaB, 
            this.colunaC, 
            this.colunaD, 
            this.colunaE, 
            this.colunaF
        ));
    }
    
    @Override
    public String toString() {
    	return toArrayList().toString();
    }
}

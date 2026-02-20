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
public class NumerosNegativos extends Numeros {

	private Integer colunaH;
	private Integer colunaI;
	private Integer colunaJ;
	private Integer colunaK;
	private Integer colunaL;
	private Integer colunaM;
	
	/**
     * Retorna os atributos da classe como um ArrayList de Integer.
     * Ideal para ser usado em comparacoes com colecoes (ex: containsAll).
     */
    public ArrayList<Integer> toArrayList() {
        // Criamos uma lista a partir dos atributos e passamos para o construtor do ArrayList
        return new ArrayList<>(Arrays.asList(
            this.colunaH, 
            this.colunaI, 
            this.colunaJ, 
            this.colunaK, 
            this.colunaL, 
            this.colunaM
        ));
    }
    
    @Override
    public String toString() {
    	return toArrayList().toString();
    }
}

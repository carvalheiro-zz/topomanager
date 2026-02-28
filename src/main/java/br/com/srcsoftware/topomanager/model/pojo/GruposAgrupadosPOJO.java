package br.com.srcsoftware.topomanager.model.pojo;

import java.util.LinkedHashSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GruposAgrupadosPOJO {
	// Definição do Mapper como constante estática (Padrão Sênior)
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private String grupoId;
	private LinkedHashSet<LinkedHashSet<Integer>> agrupamentosDeNumeros;

	@Override
	public String toString() {
		try {
			return MAPPER.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return "{erro:Falha na serialização}";
		}
	}
}
package br.com.srcsoftware.topomanager.model.pojo;

import java.util.Set;

public class GrupoPOJO {
    private final String grupoId;
    private final Set<Integer> numeros;

    public GrupoPOJO(String grupoId, Set<Integer> numeros) {
        this.grupoId = grupoId;
        this.numeros = numeros;
    }

    // Apenas Getters (Imutabilidade ajuda na performance e segurança)
    public String getGrupoId() { return grupoId; }
    public Set<Integer> getNumeros() { return numeros; }
}
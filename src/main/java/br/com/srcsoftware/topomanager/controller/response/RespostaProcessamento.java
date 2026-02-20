package br.com.srcsoftware.topomanager.controller.response;

import lombok.Data;

@Data
public class RespostaProcessamento {
	private String mensagem;
    private String tempoExecucao;
    private int totalRegistros;

    // Construtor
    public RespostaProcessamento(String mensagem, String tempoExecucao, int totalRegistros) {
        this.mensagem = mensagem;
        this.tempoExecucao = tempoExecucao;
        this.totalRegistros = totalRegistros;
    }
}

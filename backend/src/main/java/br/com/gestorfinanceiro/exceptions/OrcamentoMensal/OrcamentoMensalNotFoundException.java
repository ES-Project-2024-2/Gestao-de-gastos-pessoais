package br.com.gestorfinanceiro.exceptions.OrcamentoMensal;

public class OrcamentoMensalNotFoundException extends RuntimeException {

    public OrcamentoMensalNotFoundException(String uuid) {
        super(String.format("Orçamento mensal com UUID %s não encontrado", uuid));
    }
}

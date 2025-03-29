package br.com.gestorfinanceiro.exceptions.OrcamentoMensal;

import br.com.gestorfinanceiro.models.CategoriaEntity;
import br.com.gestorfinanceiro.models.enums.DespesasCategorias;

import java.time.YearMonth;

public class OrcamentoMensalAlreadyExistsException extends RuntimeException {

    public OrcamentoMensalAlreadyExistsException(CategoriaEntity categoria, YearMonth periodo) {
        super(String.format("Já existe um orçamento mensal para a categoria %s e o período %s", categoria, periodo));
    }
}

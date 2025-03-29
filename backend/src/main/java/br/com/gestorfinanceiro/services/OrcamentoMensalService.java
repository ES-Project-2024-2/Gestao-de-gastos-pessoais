package br.com.gestorfinanceiro.services;

import br.com.gestorfinanceiro.models.OrcamentoMensalEntity;
import br.com.gestorfinanceiro.models.enums.DespesasCategorias;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public interface OrcamentoMensalService {

    List<OrcamentoMensalEntity> listarTodosPorUsuario(String userId);
    List<OrcamentoMensalEntity> listarPorPeriodo(String userId, YearMonth periodo);
    OrcamentoMensalEntity buscarPorId(String userId, String uuid);
    OrcamentoMensalEntity criarOrcamentoMensal(String userId, String categoria, BigDecimal valorLimite, YearMonth periodo);
    OrcamentoMensalEntity atualizarOrcamentoMensal(String userId, String uuid, String categoria, BigDecimal valorLimite, YearMonth periodo);
    void excluirOrcamentoMensal(String userId, String uuid);
}

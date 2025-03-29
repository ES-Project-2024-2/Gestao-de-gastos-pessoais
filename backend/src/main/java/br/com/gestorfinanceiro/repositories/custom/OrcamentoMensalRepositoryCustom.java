package br.com.gestorfinanceiro.repositories.custom;

import br.com.gestorfinanceiro.models.CategoriaEntity;
import br.com.gestorfinanceiro.models.OrcamentoMensalEntity;
import br.com.gestorfinanceiro.models.enums.DespesasCategorias;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface OrcamentoMensalRepositoryCustom {
    Optional<OrcamentoMensalEntity> findByCategoriaAndPeriodoAndUserUuid(CategoriaEntity categoria, YearMonth periodo, String userId);
    Optional<OrcamentoMensalEntity> findByUuidAndUserUuid(String uuid, String userId);
    List<OrcamentoMensalEntity> findByUserId(String userId);
    List<OrcamentoMensalEntity> findByPeriodo(YearMonth periodo);
}

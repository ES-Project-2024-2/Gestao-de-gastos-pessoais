package br.com.gestorfinanceiro.repositories.custom.impl;

import br.com.gestorfinanceiro.models.CategoriaEntity;
import br.com.gestorfinanceiro.models.OrcamentoMensalEntity;
import br.com.gestorfinanceiro.models.enums.DespesasCategorias;
import br.com.gestorfinanceiro.repositories.custom.OrcamentoMensalRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public class OrcamentoMensalRepositoryCustomImpl implements OrcamentoMensalRepositoryCustom {

    private static final String USER_ID = "userId";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<OrcamentoMensalEntity> findByCategoriaAndPeriodoAndUserUuid(CategoriaEntity categoria, YearMonth periodo, String userId) {
        String jpql = "SELECT o FROM OrcamentoMensalEntity o WHERE o.user.uuid = :userId AND o.categoria = :categoria AND o.periodo = :periodo";

        return entityManager.createQuery(jpql, OrcamentoMensalEntity.class)
                .setParameter(USER_ID, userId)
                .setParameter("categoria", categoria)
                .setParameter("periodo", periodo)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public Optional<OrcamentoMensalEntity> findByUuidAndUserUuid(String uuid, String userId) {
        String jpql = "SELECT o FROM OrcamentoMensalEntity o WHERE o.uuid = :uuid AND o.user.uuid = :userId";

        return entityManager.createQuery(jpql, OrcamentoMensalEntity.class)
                .setParameter("uuid", uuid)
                .setParameter(USER_ID, userId)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public List<OrcamentoMensalEntity> findByUserId(String userId) {
        String jpql = "SELECT o FROM OrcamentoMensalEntity o WHERE o.user.uuid = :userId";

        return entityManager.createQuery(jpql, OrcamentoMensalEntity.class)
                .setParameter(USER_ID, userId)
                .getResultList();
    }

    @Override
    public List<OrcamentoMensalEntity> findByPeriodo(YearMonth periodo) {
        String jpql = "SELECT o FROM OrcamentoMensalEntity o WHERE o.periodo = :periodo";

        return entityManager.createQuery(jpql, OrcamentoMensalEntity.class)
                .setParameter("periodo", periodo)
                .getResultList();
    }
}

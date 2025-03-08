package br.com.gestorfinanceiro.repositories.custom.impl;

import br.com.gestorfinanceiro.models.ReceitaEntity;
import br.com.gestorfinanceiro.repositories.custom.ReceitaRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class ReceitaRepositoryCustomImpl implements ReceitaRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ReceitaEntity> findByUserAndDateRange(String userId, LocalDate inicio, LocalDate fim) {
        String jpql = "SELECT r FROM ReceitaEntity r WHERE r.user.uuid = :userId AND r.data BETWEEN :inicio AND :fim";

        TypedQuery<ReceitaEntity> query = entityManager.createQuery(jpql, ReceitaEntity.class);
        query.setParameter("userId", userId);
        query.setParameter("inicio", inicio);
        query.setParameter("fim", fim);

        return query.getResultList();
    }
}

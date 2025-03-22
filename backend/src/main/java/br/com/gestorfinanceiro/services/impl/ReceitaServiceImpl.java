package br.com.gestorfinanceiro.services.impl;

import br.com.gestorfinanceiro.dto.GraficoBarraDTO;
import br.com.gestorfinanceiro.dto.GraficoPizzaDTO;
import br.com.gestorfinanceiro.exceptions.InvalidDataException;
import br.com.gestorfinanceiro.exceptions.InvalidUserIdException;
import br.com.gestorfinanceiro.exceptions.InvalidUuidException;
import br.com.gestorfinanceiro.exceptions.receita.ReceitaNotFoundException;
import br.com.gestorfinanceiro.exceptions.receita.ReceitaOperationException;
import br.com.gestorfinanceiro.models.ReceitaEntity;
import br.com.gestorfinanceiro.models.UserEntity;
import br.com.gestorfinanceiro.repositories.ReceitaRepository;
import br.com.gestorfinanceiro.repositories.UserRepository;
import br.com.gestorfinanceiro.services.ReceitaService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReceitaServiceImpl implements ReceitaService {

    private final ReceitaRepository receitaRepository;
    private final UserRepository userRepository;

    public ReceitaServiceImpl(ReceitaRepository receitaRepository ,UserRepository userRepository) {
        this.receitaRepository = receitaRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ReceitaEntity criarReceita(ReceitaEntity receita, String userId) {
        if (receita == null) {
            throw new InvalidDataException("A receita não pode ser nula.");
        }

        if (receita.getValor() == null || receita.getValor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidDataException("O valor da receita deve ser maior que zero.");
        }

        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found."));

        try {
            receita.setUser(user);
            return receitaRepository.save(receita);
        } catch (Exception e) {
            throw new ReceitaOperationException("Erro ao criar receita. Por favor, tente novamente.", e);
        }
    }

    @Override
    public List<ReceitaEntity> listarReceitasUsuario(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new InvalidUserIdException();
        }

        List<ReceitaEntity> receitas = receitaRepository.findAllByUserUuid(userId);

        if (receitas.isEmpty()) {
            throw new ReceitaNotFoundException(userId);
        } else {
            return receitas;
        }
    }

    @Override
    public ReceitaEntity buscarReceitaPorId(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            throw new InvalidUuidException();
        }

        return receitaRepository.findById(uuid)
                .orElseThrow(() -> new ReceitaNotFoundException(uuid));
    }


    @Override
    @Transactional
    public ReceitaEntity atualizarReceita(String uuid, ReceitaEntity receitaAtualizada) {
        if (uuid == null || uuid.trim().isEmpty()) {
            throw new InvalidUuidException();
        }

        if (receitaAtualizada == null) {
            throw new InvalidDataException("Os dados da receita não podem ser nulos.");
        }

            ReceitaEntity receita = receitaRepository.findById(uuid)
                    .orElseThrow(() -> new ReceitaNotFoundException(uuid));

        try {
            receita.setData(receitaAtualizada.getData());
            receita.setCategoria(receitaAtualizada.getCategoria());
            receita.setValor(receitaAtualizada.getValor());
            receita.setOrigemDoPagamento(receitaAtualizada.getOrigemDoPagamento());
            receita.setObservacoes(receitaAtualizada.getObservacoes());

            return receitaRepository.save(receita);
        } catch (Exception e) {
            throw new ReceitaOperationException("Erro ao atualizar receita. Por favor, tente novamente.", e);
        }
    }

    @Override
    @Transactional
    public void excluirReceita(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            throw new InvalidUuidException();
        }

            ReceitaEntity receita = receitaRepository.findById(uuid)
                    .orElseThrow(() -> new ReceitaNotFoundException(uuid));

        try {
            receitaRepository.delete(receita);
        } catch (Exception e) {
            throw new ReceitaOperationException("Erro ao excluir receita. Por favor, tente novamente.", e);
        }
    }

    @Override
    public GraficoPizzaDTO gerarGraficoPizza(String userId, LocalDate inicio, LocalDate fim) {
        List<ReceitaEntity> receitas = receitaRepository.findByUserAndDateRange(userId, inicio, fim);

        Map<String, BigDecimal> categorias = receitas.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getCategoria().name(),
                        Collectors.mapping(ReceitaEntity::getValor, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        return new GraficoPizzaDTO(categorias);
    }

    @Override
    public GraficoBarraDTO gerarGraficoBarras(String userId, YearMonth inicio, YearMonth fim) {
        List<ReceitaEntity> receitas = receitaRepository.findByUserAndYearMonthRange(userId, inicio, fim);

        // Formata datas para o padrão "Mês Ano" em português
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("pt-BR"));

        // Cria um mapa ordenado com os dados mensais
        Map<String, BigDecimal> dadosMensais = new LinkedHashMap<>();
        receitas.stream()
                .collect(Collectors.groupingBy(
                        d -> YearMonth.from(d.getData()),
                        Collectors.reducing(BigDecimal.ZERO, ReceitaEntity::getValor, BigDecimal::add)))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> dadosMensais.put(e.getKey().format(formatter), e.getValue()));

        // Retorna o DTO com os dados mensais
        return new GraficoBarraDTO(dadosMensais);
    }

    @Override
    public List<ReceitaEntity> buscarReceitasPorIntervaloDeDatas(String userId, LocalDate inicio, LocalDate fim) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new InvalidUserIdException();
        }

        if (inicio == null || fim == null) {
            throw new InvalidDataException("As datas de início e fim não podem ser nulas.");
        }

        if (inicio.isAfter(fim)) {
            throw new InvalidDataException("A data de início não pode ser após a data de fim.");
        }

        try {
            return receitaRepository.findByUserAndDateRange(userId, inicio, fim);
        } catch (Exception e) {
            throw new ReceitaOperationException("Erro ao buscar receitas por intervalo de datas. Por favor, tente novamente.", e);
        }
    }

    @Override
    public List<ReceitaEntity> buscarReceitasPorIntervaloDeValores(String userId, BigDecimal min, BigDecimal max) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new InvalidUserIdException();
        }

        if (min == null || max == null) {
            throw new InvalidDataException("Os valores mínimo e máximo não podem ser nulos.");
        }

        if (min.compareTo(BigDecimal.ZERO) <= 0 || max.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidDataException("Os valores mínimo e máximo devem ser maiores que zero.");
        }

        if (min.compareTo(max) > 0) {
            throw new InvalidDataException("O valor mínimo não pode ser maior que o valor máximo.");
        }

        try {
            return receitaRepository.findByUserAndValueBetween(userId, min, max);
        } catch (Exception e) {
            throw new ReceitaOperationException("Erro ao buscar receitas por intervalo de valores. Por favor, tente novamente.", e);
        }
    }

}
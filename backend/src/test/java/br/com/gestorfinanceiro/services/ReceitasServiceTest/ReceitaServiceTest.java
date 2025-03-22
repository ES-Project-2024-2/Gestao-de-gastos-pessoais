package br.com.gestorfinanceiro.services.ReceitasServiceTest;

import br.com.gestorfinanceiro.dto.GraficoBarraDTO;
import br.com.gestorfinanceiro.dto.GraficoPizzaDTO;
import br.com.gestorfinanceiro.exceptions.InvalidDataException;
import br.com.gestorfinanceiro.exceptions.InvalidUserIdException;
import br.com.gestorfinanceiro.exceptions.InvalidUuidException;
import br.com.gestorfinanceiro.exceptions.receita.ReceitaNotFoundException;
import br.com.gestorfinanceiro.exceptions.receita.ReceitaOperationException;
import br.com.gestorfinanceiro.models.ReceitaEntity;
import br.com.gestorfinanceiro.models.UserEntity;
import br.com.gestorfinanceiro.models.enums.ReceitasCategorias;
import br.com.gestorfinanceiro.models.enums.Roles;
import br.com.gestorfinanceiro.repositories.ReceitaRepository;
import br.com.gestorfinanceiro.repositories.UserRepository;
import br.com.gestorfinanceiro.services.impl.ReceitaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceitaServiceTest {

    @Mock
    private ReceitaRepository receitaRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReceitaServiceImpl receitaService;

    private UserEntity user;
    private ReceitaEntity receita;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setUuid(UUID.randomUUID().toString());
        user.setUsername("Jorge");
        user.setEmail("jorge@gmail.com");
        user.setPassword("123456");
        user.setRole(Roles.USER);

        receita = new ReceitaEntity();
        receita.setUuid(UUID.randomUUID().toString());
        receita.setData(LocalDate.now());
        receita.setValor(BigDecimal.valueOf(10000));
        receita.setCategoria(ReceitasCategorias.SALARIO);
        receita.setOrigemDoPagamento("Empresa X");
        receita.setObservacoes("Salário do mês de janeiro");
        receita.setUser(user);
    }

    @Test
    void deveCarregarReceitaService() {
        assertNotNull(receitaService, "O ReceitaService não deveria ser nulo!");
    }

    @Nested
    class CriarReceitaTest {

        @Test
        void deveCriarReceita() {
            when(userRepository.findById(user.getUuid())).thenReturn(Optional.of(user));
            when(receitaRepository.save(any(ReceitaEntity.class))).thenReturn(receita);

            ReceitaEntity receitaSalva = receitaService.criarReceita(receita, user.getUuid());

            assertNotNull(receitaSalva);
            assertEquals(receita.getValor(), receitaSalva.getValor());
            assertEquals(receita.getCategoria(), receitaSalva.getCategoria());
        }

        @Test
        void erroAoCriarReceitaNula() {
            String userId = user.getUuid();
            assertThrows(InvalidDataException.class, () -> receitaService.criarReceita(null, userId));
        }

        @Test
        void erroAoCriarReceitaComValorInvalido() {
            receita.setValor(BigDecimal.ZERO);
            String userId = user.getUuid();
            assertThrows(InvalidDataException.class, () -> receitaService.criarReceita(receita, userId));
        }

        @Test
        void erroAoCriarReceitaComValorNulo() {
            receita.setValor(null);
            String userId = user.getUuid();
            assertThrows(InvalidDataException.class, () -> receitaService.criarReceita(receita, userId));
        }

        @Test
        void erroAoCriarReceitaComValorNegativo() {
            receita.setValor(BigDecimal.valueOf(-100));
            String userId = user.getUuid();
            assertThrows(InvalidDataException.class, () -> receitaService.criarReceita(receita, userId));
        }

        @Test
        void erroAoCriarReceitaUsuarioNaoEncontrado() {
            when(userRepository.findById(anyString())).thenReturn(Optional.empty());
            String userId = UUID.randomUUID().toString();

            assertThrows(RuntimeException.class, () -> receitaService.criarReceita(receita, userId));
        }

        @Test
        void criarReceita_DeveLancarReceitaOperationException_QuandoRepositorioFalhar() {
            // Arrange
            ReceitaEntity receitaTest = new ReceitaEntity();
            receitaTest.setValor(BigDecimal.valueOf(100));
            receitaTest.setData(LocalDate.now());
            receitaTest.setCategoria(ReceitasCategorias.SALARIO);

            UserEntity userTest = new UserEntity();
            String userId = UUID.randomUUID().toString();
            userTest.setUuid(userId);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(receitaRepository.save(any(ReceitaEntity.class))).thenThrow(new RuntimeException("Erro no repositório"));

            // Act & Assert
            assertThrows(ReceitaOperationException.class, () -> receitaService.criarReceita(receitaTest, userId));
        }
    }

    @Nested
    class ListarReceitasTest {

        @Test
        void deveListarReceitasUsuario() {
            String userId = user.getUuid();
            when(receitaRepository.findAllByUserUuid(userId)).thenReturn(List.of(receita));

            List<ReceitaEntity> receitas = receitaService.listarReceitasUsuario(userId);
            assertFalse(receitas.isEmpty());
        }

        @Test
        void erroAoListarReceitasUsuarioComIdNuloOuVazio() {
            assertThrows(InvalidUserIdException.class, () -> receitaService.listarReceitasUsuario(null));
            assertThrows(InvalidUserIdException.class, () -> receitaService.listarReceitasUsuario(""));
        }

        @Test
        void erroAoListarReceitasVazia() {
            String userId = UUID.randomUUID().toString();
            when(receitaRepository.findAllByUserUuid(anyString())).thenReturn(List.of());

            assertThrows(ReceitaNotFoundException.class, () -> receitaService.listarReceitasUsuario(userId));
        }
    }

    @Nested
    class BuscarReceitaPorIdTest {

        @Test
        void deveBuscarReceitaPorId() {
            String receitaId = receita.getUuid();
            when(receitaRepository.findById(receitaId)).thenReturn(Optional.of(receita));

            ReceitaEntity receitaBuscada = receitaService.buscarReceitaPorId(receitaId);
            assertNotNull(receitaBuscada);
        }

        @Test
        void erroAoBuscarReceitaPorIdInexistente() {
            String receitaId = UUID.randomUUID().toString();
            when(receitaRepository.findById(receitaId)).thenReturn(Optional.empty());

            assertThrows(ReceitaNotFoundException.class, () -> receitaService.buscarReceitaPorId(receitaId));
        }

        @Test
        void erroAoBuscarReceitaPorIdNuloOuVazio() {
            assertThrows(InvalidUuidException.class, () -> receitaService.buscarReceitaPorId(null));
            assertThrows(InvalidUuidException.class, () -> receitaService.buscarReceitaPorId(""));
        }
    }

    @Nested
    class AtualizarReceitaTest {

        @Test
        void deveAtualizarReceita() {
            when(receitaRepository.findById(receita.getUuid())).thenReturn(Optional.of(receita));
            when(receitaRepository.save(any(ReceitaEntity.class))).thenReturn(receita);

            receita.setValor(BigDecimal.valueOf(2000));
            ReceitaEntity receitaAtualizada = receitaService.atualizarReceita(receita.getUuid(), receita);

            assertEquals(BigDecimal.valueOf(2000), receitaAtualizada.getValor());
        }

        @Test
        void erroAoAtualizarReceitaInexistente() {
            String receitaId = UUID.randomUUID().toString();
            when(receitaRepository.findById(receitaId)).thenReturn(Optional.empty());

            assertThrows(ReceitaNotFoundException.class, () -> receitaService.atualizarReceita(receitaId, receita));
        }

        @Test
        void erroAoAtualizarUuidNullOuVazio() {
            assertThrows(InvalidUuidException.class, () -> receitaService.atualizarReceita(null, receita));
            assertThrows(InvalidUuidException.class, () -> receitaService.atualizarReceita("", receita));
        }

        @Test
        void erroAoAtualizarReceitaNula() {
            String receitaId = UUID.randomUUID().toString();

            assertThrows(InvalidDataException.class, () -> receitaService.atualizarReceita(receitaId, null));
        }

        @Test
        void atualizarReceita_DeveLancarReceitaOperationException_QuandoRepositorioFalhar() {
            // Arrange
            ReceitaEntity receitaTest = new ReceitaEntity();
            receitaTest.setUuid(UUID.randomUUID().toString());
            receitaTest.setValor(BigDecimal.valueOf(100));
            receitaTest.setData(LocalDate.now());
            receitaTest.setCategoria(ReceitasCategorias.SALARIO);

            String receitaId = receitaTest.getUuid();

            ReceitaEntity receitaAtualizada = new ReceitaEntity();
            receitaAtualizada.setValor(BigDecimal.valueOf(200));

            when(receitaRepository.findById(receitaId)).thenReturn(Optional.of(receitaTest));
            when(receitaRepository.save(any(ReceitaEntity.class))).thenThrow(new RuntimeException("Erro no repositório"));

            // Act & Assert
            assertThrows(ReceitaOperationException.class, () -> receitaService.atualizarReceita(receitaId, receitaAtualizada));
        }
    }

    @Nested
    class excluirReceitaTest {

        @Test
        void deveExcluirReceita() {
            String receitaId = receita.getUuid();
            when(receitaRepository.findById(receitaId)).thenReturn(Optional.of(receita));
            doNothing().when(receitaRepository).delete(receita);

            receitaService.excluirReceita(receitaId);
            verify(receitaRepository, times(1)).delete(receita);
        }

        @Test
        void erroAoExcluirReceitaInexistente() {
            String receitaId = UUID.randomUUID().toString();
            when(receitaRepository.findById(receitaId)).thenReturn(Optional.empty());

            assertThrows(ReceitaNotFoundException.class, () -> receitaService.excluirReceita(receitaId));
        }

        @Test
        void erroAoExcluirReceitaComIdNuloOuVazio() {
            assertThrows(InvalidUuidException.class, () -> receitaService.excluirReceita(null));
            assertThrows(InvalidUuidException.class, () -> receitaService.excluirReceita(""));
        }

        @Test
        void excluirReceita_DeveLancarReceitaOperationException_QuandoRepositorioFalhar() {
            // Arrange
            ReceitaEntity receitaTest = new ReceitaEntity();
            receitaTest.setUuid(UUID.randomUUID().toString());
            receitaTest.setValor(BigDecimal.valueOf(100));
            receitaTest.setData(LocalDate.now());
            receitaTest.setCategoria(ReceitasCategorias.SALARIO);

            String receitaId = receitaTest.getUuid();

            when(receitaRepository.findById(receitaId)).thenReturn(Optional.of(receitaTest));
            doThrow(new RuntimeException("Erro no repositório")).when(receitaRepository).delete(receitaTest);

            // Act & Assert
            assertThrows(ReceitaOperationException.class, () -> receitaService.excluirReceita(receitaId));
        }
    }

    @Nested
    class gerarGraficoTest {

        @Test
        void deveGerarGrafico() {
            when(receitaRepository.findByUserAndYearMonthRange(anyString(), any(YearMonth.class), any(YearMonth.class)))
                    .thenReturn(List.of(receita));

            YearMonth inicio = YearMonth.of(2025, 1);
            YearMonth fim = YearMonth.of(2025, 3);

            GraficoBarraDTO grafico = receitaService.gerarGraficoBarras(user.getUuid(), inicio, fim);

            assertNotNull(grafico);
            assertEquals(1, grafico.dadosMensais().size());
            assertEquals(BigDecimal.valueOf(10000), grafico.dadosMensais().get("março 2025"));
        }

        @Test
        void deveGerarGraficoPizza() {
            when(receitaRepository.findByUserAndDateRange(anyString(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of(receita));

            LocalDate inicio = LocalDate.of(2024, 1, 1);
            LocalDate fim = LocalDate.of(2024, 12, 31);

            GraficoPizzaDTO grafico = receitaService.gerarGraficoPizza(user.getUuid(), inicio, fim);

            assertNotNull(grafico);
            assertEquals(1, grafico.categorias().size());
            assertEquals(BigDecimal.valueOf(10000).stripTrailingZeros(), grafico.categorias().get("SALARIO").stripTrailingZeros());
        }
    }

    @Nested
    class buscaAvancadaTest {

        @Test
        void deveBuscarReceitasPorIntervaloDeDatas() {
            when(receitaRepository.findByUserAndDateRange(anyString(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of(receita));

            LocalDate inicio = LocalDate.of(2024, 1, 1);
            LocalDate fim = LocalDate.of(2024, 2, 18);

            List<ReceitaEntity> receitas = receitaService.buscarReceitasPorIntervaloDeDatas(user.getUuid(), inicio, fim);

            assertNotNull(receitas);
            assertEquals(1, receitas.size());
            assertEquals(BigDecimal.valueOf(10000).stripTrailingZeros(), receitas.get(0).getValor().stripTrailingZeros());
        }

        @Test
        void erroAoBuscarReceitasPorIntervaloDeDatasComUserIdNuloOuVazio() {
            LocalDate inicio = LocalDate.of(2024, 1, 1);
            LocalDate fim = LocalDate.of(2024, 2, 18);

            assertThrows(InvalidUserIdException.class, () -> receitaService.buscarReceitasPorIntervaloDeDatas(null, inicio, fim));
            assertThrows(InvalidUserIdException.class, () -> receitaService.buscarReceitasPorIntervaloDeDatas("", inicio, fim));
        }

        @Test
        void erroAoBuscarReceitasPorIntervaloDeDatasComDatasNulas() {
            String userId = user.getUuid();
            LocalDate inicio = LocalDate.of(2024, 1, 1);
            LocalDate fim = LocalDate.of(2024, 2, 18);
            assertThrows(InvalidDataException.class, () -> receitaService.buscarReceitasPorIntervaloDeDatas(userId, null, fim));
            assertThrows(InvalidDataException.class, () -> receitaService.buscarReceitasPorIntervaloDeDatas(userId, inicio, null));
        }

        @Test
        void erroAoBuscarReceitassPorIntervaloDeDatasComInicioDepoisDoFim() {
            String userId = user.getUuid();
            LocalDate inicio = LocalDate.of(2024, 1, 10);
            LocalDate fim = LocalDate.of(2024, 1, 5);

            assertThrows(InvalidDataException.class, () -> receitaService.buscarReceitasPorIntervaloDeDatas(userId, inicio, fim));
        }

        @Test
        void buscarReceitasPorIntervaloDeDatas_DeveLancarReceitaOperationException_QuandoRepositorioFalhar() {
            // Arrange
            String userId = UUID.randomUUID().toString();
            LocalDate inicio = LocalDate.of(2024, 1, 1);
            LocalDate fim = LocalDate.of(2024, 12, 31);

            when(receitaRepository.findByUserAndDateRange(userId, inicio, fim)).thenThrow(new RuntimeException("Erro no repositório"));

            // Act & Assert
            assertThrows(ReceitaOperationException.class, () -> receitaService.buscarReceitasPorIntervaloDeDatas(userId, inicio, fim));
        }

        @Test
        void deveBuscarReceitasPorIntervaloDeValores() {
            when(receitaRepository.findByUserAndValueBetween(anyString(), any(BigDecimal.class), any(BigDecimal.class)))
                    .thenReturn(List.of(receita));

            BigDecimal min = BigDecimal.valueOf(100);
            BigDecimal max = BigDecimal.valueOf(150);

            List<ReceitaEntity> receitas = receitaService.buscarReceitasPorIntervaloDeValores(user.getUuid(), min, max);

            assertNotNull(receitas);
            assertEquals(1, receitas.size());
            assertEquals(BigDecimal.valueOf(10000).stripTrailingZeros(), receitas.get(0).getValor().stripTrailingZeros());
        }

        @Test
        void erroAoBuscarReceitasPorIntervaloDeValoresComUserIdNuloOuVazio() {
            BigDecimal min = BigDecimal.valueOf(100);
            BigDecimal max = BigDecimal.valueOf(150);

            assertThrows(InvalidUserIdException.class, () -> receitaService.buscarReceitasPorIntervaloDeValores(null, min, max));
            assertThrows(InvalidUserIdException.class, () -> receitaService.buscarReceitasPorIntervaloDeValores("", min, max));
        }

        @Test
        void erroAoBuscarReceitassPorIntervaloDeValoresComValoresNulos() {
            String userId = user.getUuid();
            BigDecimal min = BigDecimal.valueOf(100);
            BigDecimal max = BigDecimal.valueOf(150);

            assertThrows(InvalidDataException.class, () -> receitaService.buscarReceitasPorIntervaloDeValores(userId, null, max));
            assertThrows(InvalidDataException.class, () -> receitaService.buscarReceitasPorIntervaloDeValores(userId, min, null));
        }

        @Test
        void erroAoBuscarReceitasPorIntervaloDeValoresComValoresMenoresOuIguaisAZero() {
            String userId = user.getUuid();
            BigDecimal min = BigDecimal.valueOf(100);
            BigDecimal max = BigDecimal.valueOf(150);

            assertThrows(InvalidDataException.class, () -> receitaService.buscarReceitasPorIntervaloDeValores(userId, BigDecimal.ZERO, max));
            assertThrows(InvalidDataException.class, () -> receitaService.buscarReceitasPorIntervaloDeValores(userId, min, BigDecimal.ZERO));
        }

        @Test
        void erroAoBuscarReceitassPorIntervaloDeValoresComMinMaiorQueMax() {
            String userId = user.getUuid();
            BigDecimal min = BigDecimal.valueOf(200);
            BigDecimal max = BigDecimal.valueOf(100);

            assertThrows(InvalidDataException.class, () -> receitaService.buscarReceitasPorIntervaloDeValores(userId, min, max));
        }

        @Test
        void buscarReceitasPorIntervaloDeValores_DeveLancarReceitaOperationException_QuandoRepositorioFalhar() {
            // Arrange
            String userId = UUID.randomUUID().toString();
            BigDecimal min = BigDecimal.valueOf(100);
            BigDecimal max = BigDecimal.valueOf(200);

            when(receitaRepository.findByUserAndValueBetween(userId, min, max)).thenThrow(new RuntimeException("Erro no repositório"));

            // Act & Assert
            assertThrows(ReceitaOperationException.class, () -> receitaService.buscarReceitasPorIntervaloDeValores(userId, min, max));
        }
    }
}
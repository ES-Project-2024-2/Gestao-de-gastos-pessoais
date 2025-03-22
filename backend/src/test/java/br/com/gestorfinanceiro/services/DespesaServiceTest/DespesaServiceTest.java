package br.com.gestorfinanceiro.services.DespesaServiceTest;

import br.com.gestorfinanceiro.dto.GraficoBarraDTO;
import br.com.gestorfinanceiro.dto.GraficoPizzaDTO;
import br.com.gestorfinanceiro.exceptions.InvalidDataException;
import br.com.gestorfinanceiro.exceptions.InvalidUserIdException;
import br.com.gestorfinanceiro.exceptions.InvalidUuidException;
import br.com.gestorfinanceiro.exceptions.despesa.DespesaNotFoundException;
import br.com.gestorfinanceiro.exceptions.despesa.DespesaOperationException;
import br.com.gestorfinanceiro.models.DespesaEntity;
import br.com.gestorfinanceiro.models.UserEntity;
import br.com.gestorfinanceiro.models.enums.DespesasCategorias;
import br.com.gestorfinanceiro.models.enums.Roles;
import br.com.gestorfinanceiro.repositories.DespesaRepository;
import br.com.gestorfinanceiro.repositories.UserRepository;
import br.com.gestorfinanceiro.services.impl.DespesaServiceImpl;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DespesaServiceTest {

    @Mock
    private DespesaRepository despesaRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DespesaServiceImpl despesaService;

    private UserEntity user;
    private DespesaEntity despesa;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setUuid(UUID.randomUUID().toString());
        user.setUsername("Jorge");
        user.setEmail("jorge@gmail.com");
        user.setPassword("123456");
        user.setRole(Roles.USER);

        despesa = new DespesaEntity();
        despesa.setUuid(UUID.randomUUID().toString());
        despesa.setData(LocalDate.now());
        despesa.setValor(BigDecimal.valueOf(100));
        despesa.setCategoria(DespesasCategorias.ALIMENTACAO);
        despesa.setDestinoPagamento("Mercado");
        despesa.setObservacoes("Compras do mês");
        despesa.setUser(user);
    }

    @Test
    void deveCarregarDespesaService() {
        assertNotNull(despesaService, "O DespesaService não deveria ser nulo!");
    }

    @Nested
    class CriarDespesaTest {

        @Test
        void deveCriarDespesa() {
            when(userRepository.findById(user.getUuid())).thenReturn(Optional.of(user));
            when(despesaRepository.save(any(DespesaEntity.class))).thenReturn(despesa);

            DespesaEntity despesaSalva = despesaService.criarDespesa(despesa, user.getUuid());

            assertNotNull(despesaSalva);
            assertEquals(despesa.getValor(), despesaSalva.getValor());
            assertEquals(despesa.getCategoria(), despesaSalva.getCategoria());
        }

        @Test
        void erroAoCriarDespesaNula() {
            String userId = user.getUuid();
            assertThrows(InvalidDataException.class, () -> despesaService.criarDespesa(null, userId));
        }

        @Test
        void erroAoCriarDespesaComValorInvalido() {
            despesa.setValor(BigDecimal.ZERO);
            String userId = user.getUuid();
            assertThrows(InvalidDataException.class, () -> despesaService.criarDespesa(despesa, userId));
        }

        @Test
        void erroAoCriarDespesaComValorNulo() {
            despesa.setValor(null);
            String userId = user.getUuid();
            assertThrows(InvalidDataException.class, () -> despesaService.criarDespesa(despesa, userId));
        }

        @Test
        void erroAoCriarDespesaComValorNegativo() {
            despesa.setValor(BigDecimal.valueOf(-100));
            String userId = user.getUuid();
            assertThrows(InvalidDataException.class, () -> despesaService.criarDespesa(despesa, userId));
        }

        @Test
        void erroAoCriarDespesaUsuarioNaoEncontrado() {
            when(userRepository.findById(anyString())).thenReturn(Optional.empty());
            String userId = UUID.randomUUID().toString();

            assertThrows(RuntimeException.class, () -> despesaService.criarDespesa(despesa, userId));
        }

        @Test
        void criarDespesa_DeveLancarDespesaOperationException_QuandoRepositorioFalhar() {
            // Arrange
            DespesaEntity despesaTest = new DespesaEntity();
            despesaTest.setValor(BigDecimal.valueOf(100));
            despesaTest.setData(LocalDate.now());
            despesaTest.setCategoria(DespesasCategorias.ALIMENTACAO);

            UserEntity userTest = new UserEntity();
            String userId = UUID.randomUUID().toString();
            userTest.setUuid(userId);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(despesaRepository.save(any(DespesaEntity.class))).thenThrow(new RuntimeException("Erro no repositório"));

            // Act & Assert
            assertThrows(DespesaOperationException.class, () -> despesaService.criarDespesa(despesaTest, userId));
        }
    }

    @Nested
    class ListarDespesasTest {

        @Test
        void deveListarDespesasUsuario() {
            String userId = user.getUuid();
            when(despesaRepository.findAllByUserUuid(userId)).thenReturn(List.of(despesa));

            List<DespesaEntity> despesas = despesaService.listarDespesasUsuario(userId);
            assertFalse(despesas.isEmpty());
        }

        @Test
        void erroAoListarDespesasUsuarioComIdNuloOuVazio() {
            assertThrows(InvalidUserIdException.class, () -> despesaService.listarDespesasUsuario(null));
            assertThrows(InvalidUserIdException.class, () -> despesaService.listarDespesasUsuario(""));
        }

        @Test
        void erroAoListarDespesasVazia() {
            String userId = UUID.randomUUID().toString();
            when(despesaRepository.findAllByUserUuid(anyString())).thenReturn(List.of());

            assertThrows(DespesaNotFoundException.class, () -> despesaService.listarDespesasUsuario(userId));
        }
    }

    @Nested
    class BuscarDespesaPorIdTest {

        @Test
        void deveBuscarDespesaPorId() {
            when(despesaRepository.findById(despesa.getUuid())).thenReturn(Optional.of(despesa));

            DespesaEntity despesaEncontrada = despesaService.buscarDespesaPorId(despesa.getUuid());
            assertEquals(despesa.getUuid(), despesaEncontrada.getUuid());
        }

        @Test
        void erroAoBuscarDespesaPorIdInexistente() {
            String userId = UUID.randomUUID().toString();
            when(despesaRepository.findById(anyString())).thenReturn(Optional.empty());

            assertThrows(DespesaNotFoundException.class, () -> despesaService.buscarDespesaPorId(userId));
        }

        @Test
        void erroAoBuscarDespesaPorIdNuloOuVazio() {
            assertThrows(InvalidUuidException.class, () -> despesaService.buscarDespesaPorId(null));
            assertThrows(InvalidUuidException.class, () -> despesaService.buscarDespesaPorId(""));
        }
    }

    @Nested
    class AtualizarDespesaTest {

        @Test
        void deveAtualizarDespesa() {
            when(despesaRepository.findById(despesa.getUuid())).thenReturn(Optional.of(despesa));
            when(despesaRepository.save(any(DespesaEntity.class))).thenReturn(despesa);

            despesa.setValor(BigDecimal.valueOf(200));
            DespesaEntity despesaAtualizada = despesaService.atualizarDespesa(despesa.getUuid(), despesa);

            assertEquals(BigDecimal.valueOf(200), despesaAtualizada.getValor());
        }

        @Test
        void erroAoAtualizarDespesaInexistente() {
            String userId = UUID.randomUUID().toString();
            when(despesaRepository.findById(anyString())).thenReturn(Optional.empty());

            assertThrows(DespesaNotFoundException.class, () -> despesaService.atualizarDespesa(userId, despesa));
        }

        @Test
        void erroAoAtualizarUuidNuloOuVazio() {
            assertThrows(InvalidUuidException.class, () -> despesaService.atualizarDespesa(null, despesa));
            assertThrows(InvalidUuidException.class, () -> despesaService.atualizarDespesa("", despesa));
        }

        @Test
        void erroAoAtualizarDespesaNula() {
            String despesaId = UUID.randomUUID().toString();

            assertThrows(InvalidDataException.class, () -> despesaService.atualizarDespesa(despesaId, null));
        }


        @Test
        void atualizarDespesa_DeveLancarDespesaOperationException_QuandoRepositorioFalhar() {
            // Arrange
            DespesaEntity despesaTest = new DespesaEntity();
            despesaTest.setUuid(UUID.randomUUID().toString());
            despesaTest.setValor(BigDecimal.valueOf(100));
            despesaTest.setData(LocalDate.now());
            despesaTest.setCategoria(DespesasCategorias.ALIMENTACAO);

            String despesaId = despesaTest.getUuid();

            DespesaEntity despesaAtualizada = new DespesaEntity();
            despesaAtualizada.setValor(BigDecimal.valueOf(200));

            when(despesaRepository.findById(despesaId)).thenReturn(Optional.of(despesaTest));
            when(despesaRepository.save(any(DespesaEntity.class))).thenThrow(new RuntimeException("Erro no repositório"));

            // Act & Assert
            assertThrows(DespesaOperationException.class, () -> despesaService.atualizarDespesa(despesaId, despesaAtualizada));
        }
    }

    @Nested
    class ExcluirDespesaTest {

        @Test
        void deveExcluirDespesa() {
            String despesaId = despesa.getUuid();
            when(despesaRepository.findById(despesaId)).thenReturn(Optional.of(despesa));
            doNothing().when(despesaRepository).delete(despesa); // Verifica o método delete

            despesaService.excluirDespesa(despesaId);
            verify(despesaRepository, times(1)).delete(despesa); // Verifica o método delete
        }

        @Test
        void erroAoExcluirDespesaInexistente() {
            String despesaId = UUID.randomUUID().toString();
            when(despesaRepository.findById(anyString())).thenReturn(Optional.empty());

            assertThrows(DespesaNotFoundException.class, () -> despesaService.excluirDespesa(despesaId));
        }

        @Test
        void erroAoExcluirDespesaComUuidNuloOuVazio() {
            assertThrows(InvalidUuidException.class, () -> despesaService.excluirDespesa(null));
            assertThrows(InvalidUuidException.class, () -> despesaService.excluirDespesa(""));
        }

        @Test
        void excluirDespesa_DeveLancarDespesaOperationException_QuandoRepositorioFalhar() {
            // Arrange
            DespesaEntity despesaTest = new DespesaEntity();
            despesaTest.setUuid(UUID.randomUUID().toString());

            String despesaId = despesaTest.getUuid();

            when(despesaRepository.findById(despesaId)).thenReturn(Optional.of(despesaTest));
            doThrow(new RuntimeException("Erro no repositório")).when(despesaRepository).delete(despesaTest);

            // Act & Assert
            assertThrows(DespesaOperationException.class, () -> despesaService.excluirDespesa(despesaId));
        }
    }

    @Nested
    class GerarGraficosTest {

        @Test
        void deveGerarGraficoBarras() {
            when(despesaRepository.findByUserAndYearMonthRange(anyString(), any(YearMonth.class), any(YearMonth.class)))
                    .thenReturn(List.of(despesa));

            YearMonth inicio = YearMonth.of(2025, 1);
            YearMonth fim = YearMonth.of(2025, 3);

            GraficoBarraDTO grafico = despesaService.gerarGraficoBarras(user.getUuid(), inicio, fim);

            assertNotNull(grafico);
            assertEquals(1, grafico.dadosMensais().size());
            assertEquals(BigDecimal.valueOf(100), grafico.dadosMensais().get("março 2025"));
        }

        @Test
        void deveGerarGraficoPizza() {
            when(despesaRepository.findByUserAndDateRange(anyString(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of(despesa));

            LocalDate inicio = LocalDate.of(2024, 1, 1);
            LocalDate fim = LocalDate.of(2024, 12, 31);

            GraficoPizzaDTO grafico = despesaService.gerarGraficoPizza(user.getUuid(), inicio, fim);

            assertNotNull(grafico);
            assertEquals(1, grafico.categorias().size());
            assertEquals(BigDecimal.valueOf(100).stripTrailingZeros(), grafico.categorias().get("ALIMENTACAO").stripTrailingZeros());
        }
    }

    @Nested
    class BuscaAvancadaTest {
        @Test
        void deveBuscarDespesasPorIntervaloDeDatas() {
            when(despesaRepository.findByUserAndDateRange(anyString(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of(despesa));

            LocalDate inicio = LocalDate.of(2024, 1, 1);
            LocalDate fim = LocalDate.of(2024, 2, 18);

            List<DespesaEntity> despesas = despesaService.buscarDespesasPorIntervaloDeDatas(user.getUuid(), inicio, fim);

            assertNotNull(despesas);
            assertEquals(1, despesas.size());
            assertEquals(BigDecimal.valueOf(100).stripTrailingZeros(), despesas.get(0).getValor().stripTrailingZeros());
        }

        @Test
        void erroAoBuscarDespesasPorIntervaloDeDatasComUserIdNuloOuVazio() {
            LocalDate inicio = LocalDate.of(2024, 1, 1);
            LocalDate fim = LocalDate.of(2024, 2, 18);

            assertThrows(InvalidUserIdException.class, () -> despesaService.buscarDespesasPorIntervaloDeDatas(null, inicio, fim));
            assertThrows(InvalidUserIdException.class, () -> despesaService.buscarDespesasPorIntervaloDeDatas("", inicio, fim));
        }

        @Test
        void erroAoBuscarDespesasPorIntervaloDeDatasComDatasNulas() {
            String userId = user.getUuid();
            LocalDate inicio = LocalDate.of(2024, 1, 1);
            LocalDate fim = LocalDate.of(2024, 2, 18);
            assertThrows(InvalidDataException.class, () -> despesaService.buscarDespesasPorIntervaloDeDatas(userId, null, fim));
            assertThrows(InvalidDataException.class, () -> despesaService.buscarDespesasPorIntervaloDeDatas(userId, inicio, null));
        }

        @Test
        void erroAoBuscarDespesasPorIntervaloDeDatasComInicioDepoisDoFim() {
            String userId = user.getUuid();
            LocalDate inicio = LocalDate.of(2024, 1, 10);
            LocalDate fim = LocalDate.of(2024, 1, 5);

            assertThrows(InvalidDataException.class, () -> despesaService.buscarDespesasPorIntervaloDeDatas(userId, inicio, fim));
        }

        @Test
        void buscarDespesasPorIntervaloDeDatas_DeveLancarDespesaOperationException_QuandoRepositorioFalhar() {
            // Arrange
            String userId = UUID.randomUUID().toString();
            LocalDate inicio = LocalDate.of(2024, 1, 1);
            LocalDate fim = LocalDate.of(2024, 12, 31);

            when(despesaRepository.findByUserAndDateRange(userId, inicio, fim)).thenThrow(new RuntimeException("Erro no repositório"));

            // Act & Assert
            assertThrows(DespesaOperationException.class, () -> despesaService.buscarDespesasPorIntervaloDeDatas(userId, inicio, fim));
        }

        @Test
        void deveBuscarDespesasPorIntervaloDeValores() {
            when(despesaRepository.findByUserAndValueBetween(anyString(), any(BigDecimal.class), any(BigDecimal.class)))
                    .thenReturn(List.of(despesa));

            BigDecimal min = BigDecimal.valueOf(100);
            BigDecimal max = BigDecimal.valueOf(150);

            List<DespesaEntity> despesas = despesaService.buscarDespesasPorIntervaloDeValores(user.getUuid(), min, max);

            assertNotNull(despesas);
            assertEquals(1, despesas.size());
            assertEquals(BigDecimal.valueOf(100).stripTrailingZeros(), despesas.get(0).getValor().stripTrailingZeros());
        }

        @Test
        void erroAoBuscarDespesasPorIntervaloDeValoresComUserIdNuloOuVazio() {
            BigDecimal min = BigDecimal.valueOf(100);
            BigDecimal max = BigDecimal.valueOf(150);

            assertThrows(InvalidUserIdException.class, () -> despesaService.buscarDespesasPorIntervaloDeValores(null, min, max));
            assertThrows(InvalidUserIdException.class, () -> despesaService.buscarDespesasPorIntervaloDeValores("", min, max));
        }

        @Test
        void erroAoBuscarDespesasPorIntervaloDeValoresComValoresNulos() {
            String userId = user.getUuid();
            BigDecimal min = BigDecimal.valueOf(100);
            BigDecimal max = BigDecimal.valueOf(150);

            assertThrows(InvalidDataException.class, () -> despesaService.buscarDespesasPorIntervaloDeValores(userId, null, max));
            assertThrows(InvalidDataException.class, () -> despesaService.buscarDespesasPorIntervaloDeValores(userId, min, null));
        }

        @Test
        void erroAoBuscarDespesasPorIntervaloDeValoresComValoresMenoresOuIguaisAZero() {
            String userId = user.getUuid();
            BigDecimal min = BigDecimal.valueOf(100);
            BigDecimal max = BigDecimal.valueOf(150);

            assertThrows(InvalidDataException.class, () -> despesaService.buscarDespesasPorIntervaloDeValores(userId, BigDecimal.ZERO, max));
            assertThrows(InvalidDataException.class, () -> despesaService.buscarDespesasPorIntervaloDeValores(userId, min, BigDecimal.ZERO));
        }

        @Test
        void erroAoBuscarDespesasPorIntervaloDeValoresComMinMaiorQueMax() {
            String userId = user.getUuid();
            BigDecimal min = BigDecimal.valueOf(200);
            BigDecimal max = BigDecimal.valueOf(100);

            assertThrows(InvalidDataException.class, () -> despesaService.buscarDespesasPorIntervaloDeValores(userId, min, max));
        }

        @Test
        void buscarDespesasPorIntervaloDeValores_DeveLancarDespesaOperationException_QuandoRepositorioFalhar() {
            // Arrange
            String userId = UUID.randomUUID().toString();
            BigDecimal min = BigDecimal.valueOf(100);
            BigDecimal max = BigDecimal.valueOf(200);

            when(despesaRepository.findByUserAndValueBetween(userId, min, max)).thenThrow(new RuntimeException("Erro no repositório"));

            // Act & Assert
            assertThrows(DespesaOperationException.class, () -> despesaService.buscarDespesasPorIntervaloDeValores(userId, min, max));
        }
    }
}
package br.com.gestorfinanceiro.controllers.DashboardControllerTest;
import br.com.gestorfinanceiro.dto.despesa.DespesaDTO;
import br.com.gestorfinanceiro.dto.orcamentomensal.OrcamentoMensalDTO;
import br.com.gestorfinanceiro.dto.receita.ReceitaDTO;
import br.com.gestorfinanceiro.controller.DashboardController;
import br.com.gestorfinanceiro.controller.OrcamentoMensalController;
import br.com.gestorfinanceiro.controller.OrcamentoMensalController.OrcamentoMensalRequest;
import br.com.gestorfinanceiro.models.CategoriaEntity;
import br.com.gestorfinanceiro.models.DespesaEntity;
import br.com.gestorfinanceiro.models.OrcamentoMensalEntity;
import br.com.gestorfinanceiro.models.ReceitaEntity;
import br.com.gestorfinanceiro.models.UserEntity;
import br.com.gestorfinanceiro.models.enums.CategoriaType;
import br.com.gestorfinanceiro.services.DashboardService;
import br.com.gestorfinanceiro.services.OrcamentoMensalService;
import io.jsonwebtoken.JwtException;
import br.com.gestorfinanceiro.config.security.JwtUtil;
import br.com.gestorfinanceiro.mappers.Mapper;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ExtendWith(MockitoExtension.class)
class DashboardControllerUnitTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private DashboardService dashboardService;

    @Mock
    private Mapper<DespesaEntity, DespesaDTO> despesaMapper;

    @Mock
    private Mapper<ReceitaEntity, ReceitaDTO> receitaMapper;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private DashboardController dashboardController;

    private MockMvc mockMvc;

    private UserEntity user;
    private YearMonth periodo;
    private DespesaEntity despesa;
    private ReceitaEntity receita;
    private DespesaDTO despesaDTO;
    private ReceitaDTO receitaDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        user = new UserEntity();
        user.setUuid(UUID.randomUUID().toString());
        user.setUsername("Vini");
        user.setEmail("vini@gmail.com");
        user.setPassword("123456");

        periodo = YearMonth.now();

        despesa = new DespesaEntity();
        despesa.setUuid(UUID.randomUUID().toString());
        despesa.setDestinoPagamento("Supermercado");
        despesa.setValor(BigDecimal.valueOf(500));
        despesa.setData(LocalDate.now());
        despesa.setUser(user);

        receita = new ReceitaEntity();
        receita.setUuid(UUID.randomUUID().toString());
        receita.setOrigemDoPagamento("Salário");
        receita.setValor(BigDecimal.valueOf(3000));
        receita.setData(LocalDate.now());
        receita.setUser(user);

        despesaDTO = new DespesaDTO();
        despesaDTO.setUuid(despesa.getUuid());
        despesaDTO.setDestinoPagamento("Supermercado");
        despesaDTO.setValor(BigDecimal.valueOf(500));

        receitaDTO = new ReceitaDTO();
        receitaDTO.setUuid(receita.getUuid());
        receitaDTO.setOrigemDoPagamento("Salário");
        receitaDTO.setValor(BigDecimal.valueOf(3000));
    }

    @Test
    void deveCarregarDashboardController() {
        assertNotNull(dashboardController, "O DashboardController não deveria ser nulo!");
    }

    @Nested
    class SaldoTotalTest {
        @Test
        void deveRetornarSaldoTotal() throws Exception {
            BigDecimal saldo = BigDecimal.valueOf(2500);
            when(jwtUtil.extractUserId(anyString())).thenReturn(user.getUuid());
            when(dashboardService.getSaldoTotal(user.getUuid(), periodo)).thenReturn(saldo);

            mockMvc.perform(get("/dashboard/saldo-total")
                    .param("periodo", periodo.toString())
                    .header("Authorization", "Bearer token_valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo").value(periodo.toString()))
                .andExpect(jsonPath("$.saldo").value(2500));
        }

        @Test
        void deveRetornarBadRequestQuandoPeriodoNaoInformado() throws Exception {
            mockMvc.perform(get("/dashboard/saldo-total")
                    .header("Authorization", "Bearer token_valido"))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class MaiorDespesaTest {
        @Test
        void deveRetornarMaiorDespesa() throws Exception {
            when(jwtUtil.extractUserId(anyString())).thenReturn(user.getUuid());
            when(dashboardService.getMaiorDespesa(user.getUuid(), periodo)).thenReturn(despesa);
            when(despesaMapper.mapTo(despesa)).thenReturn(despesaDTO);

            mockMvc.perform(get("/dashboard/maior-despesa")
                    .param("periodo", periodo.toString())
                    .header("Authorization", "Bearer token_valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value(500));
        }

        @Test
        void deveRetornarNotFoundQuandoNaoExistirDespesa() throws Exception {
            when(jwtUtil.extractUserId(anyString())).thenReturn(user.getUuid());
            when(dashboardService.getMaiorDespesa(user.getUuid(), periodo)).thenReturn(null);

            mockMvc.perform(get("/dashboard/maior-despesa")
                    .param("periodo", periodo.toString())
                    .header("Authorization", "Bearer token_valido"))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    class MaiorReceitaTest {
        @Test
        void deveRetornarMaiorReceita() throws Exception {
            when(jwtUtil.extractUserId(anyString())).thenReturn(user.getUuid());
            when(dashboardService.getMaiorReceita(user.getUuid(), periodo)).thenReturn(receita);
            when(receitaMapper.mapTo(receita)).thenReturn(receitaDTO);

            mockMvc.perform(get("/dashboard/maior-receita")
                    .param("periodo", periodo.toString())
                    .header("Authorization", "Bearer token_valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value(3000));
        }

        @Test
        void deveRetornarNotFoundQuandoNaoExistirReceita() throws Exception {
            when(jwtUtil.extractUserId(anyString())).thenReturn(user.getUuid());
            when(dashboardService.getMaiorReceita(user.getUuid(), periodo)).thenReturn(null);

            mockMvc.perform(get("/dashboard/maior-receita")
                    .param("periodo", periodo.toString())
                    .header("Authorization", "Bearer token_valido"))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    class CategoriaMaiorDespesaTest {
        @Test
        void deveRetornarCategoriaComMaiorDespesa() throws Exception {
            Map<String, BigDecimal> categoria = Map.of("ALIMENTACAO", BigDecimal.valueOf(1000));
            
            when(jwtUtil.extractUserId(anyString())).thenReturn(user.getUuid());
            when(dashboardService.getCategoriaComMaiorDespesa(user.getUuid(), periodo)).thenReturn(categoria);

            mockMvc.perform(get("/dashboard/categoria-maior-despesa")
                    .param("periodo", periodo.toString())
                    .header("Authorization", "Bearer token_valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ALIMENTACAO").value(1000));
        }
    }

    @Nested
    class TotalDespesasTest {
        @Test
        void deveRetornarTotalDespesas() throws Exception {
            BigDecimal totalDespesas = BigDecimal.valueOf(1500);
            
            when(jwtUtil.extractUserId(anyString())).thenReturn(user.getUuid());
            when(dashboardService.calcularTotalDespesasNoMes(user.getUuid(), periodo)).thenReturn(totalDespesas);

            mockMvc.perform(get("/dashboard/despesa-total")
                    .param("periodo", periodo.toString())
                    .header("Authorization", "Bearer token_valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo").value(periodo.toString()))
                .andExpect(jsonPath("$.saldo").value(1500));
        }
    }

    @Nested
    class TotalReceitasTest {
        @Test
        void deveRetornarTotalReceitas() throws Exception {
            BigDecimal totalReceitas = BigDecimal.valueOf(4000);
            
            when(jwtUtil.extractUserId(anyString())).thenReturn(user.getUuid());
            when(dashboardService.calcularTotalReceitasNoMes(user.getUuid(), periodo)).thenReturn(totalReceitas);

            mockMvc.perform(get("/dashboard/receita-total")
                    .param("periodo", periodo.toString())
                    .header("Authorization", "Bearer token_valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo").value(periodo.toString()))
                .andExpect(jsonPath("$.saldo").value(4000));
        }
    }

    @Test
    void deveExtrairTokenCorretamente() throws Exception {
        String expectedToken = "token_esperado";
        when(jwtUtil.extractUserId(expectedToken)).thenReturn(user.getUuid());
        when(dashboardService.getSaldoTotal(user.getUuid(), periodo)).thenReturn(BigDecimal.ZERO);

        mockMvc.perform(get("/dashboard/saldo-total")
                .param("periodo", periodo.toString())
                .header("Authorization", "Bearer " + expectedToken));

        verify(jwtUtil).extractUserId(expectedToken);
    }
}
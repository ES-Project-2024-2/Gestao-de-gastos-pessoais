package br.com.gestorfinanceiro.controllers.OrcamentoMensalControllerTest;

import br.com.gestorfinanceiro.dto.orcamentomensal.OrcamentoMensalDTO;
import br.com.gestorfinanceiro.controller.OrcamentoMensalController;
import br.com.gestorfinanceiro.controller.OrcamentoMensalController.OrcamentoMensalRequest;
import br.com.gestorfinanceiro.models.CategoriaEntity;
import br.com.gestorfinanceiro.models.OrcamentoMensalEntity;
import br.com.gestorfinanceiro.models.UserEntity;
import br.com.gestorfinanceiro.models.enums.CategoriaType;
import br.com.gestorfinanceiro.services.OrcamentoMensalService;
import br.com.gestorfinanceiro.config.security.JwtUtil;
import br.com.gestorfinanceiro.mappers.Mapper;

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
import java.time.YearMonth;
import java.util.UUID;
import java.util.List;

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
class OrcamentoMensalControllerUnitTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private OrcamentoMensalService orcamentoMensalService;

    @Mock
    private Mapper<OrcamentoMensalEntity, OrcamentoMensalDTO> orcamentoMensalMapper;

    @Mock
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;

    @InjectMocks
    private OrcamentoMensalController orcamentoMensalController;

    private UserEntity user;
    private CategoriaEntity categoria;
    private OrcamentoMensalEntity orcamento;
    private OrcamentoMensalDTO orcamentoDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    
        mockMvc = MockMvcBuilders.standaloneSetup(orcamentoMensalController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    
        user = new UserEntity();
        user.setUuid(UUID.randomUUID().toString());
        user.setUsername("Vini");
        user.setEmail("vini@gmail.com");
        user.setPassword("123456");
        
        categoria = new CategoriaEntity(
            "ALIMENTACAO",
            CategoriaType.DESPESAS,
            user
        );
        categoria.setUuid(UUID.randomUUID().toString());
    
        orcamento = new OrcamentoMensalEntity();
        orcamento.setUuid(UUID.randomUUID().toString());
        orcamento.setCategoria(categoria);
        orcamento.setValorLimite(BigDecimal.valueOf(1000));
        orcamento.setPeriodo(YearMonth.now());
        orcamento.setUser(user);
    
        orcamentoDTO = new OrcamentoMensalDTO();
        orcamentoDTO.setUuid(orcamento.getUuid());
        orcamentoDTO.setCategoria(categoria.getNome());
        orcamentoDTO.setValorLimite(BigDecimal.valueOf(1000));
        orcamentoDTO.setPeriodo(YearMonth.now());
    }

    @Test
    void deveCarregarOrcamentoMensalController() {
        assertNotNull(orcamentoMensalController, "O OrcamentoMensalController não deveria ser nulo!");
    }

    @Nested
    class ListarOrcamentosTest {
        @Test
        void deveListarTodosOrcamentos() throws Exception {
            when(jwtUtil.extractUserId(anyString())).thenReturn(user.getUuid());
            when(orcamentoMensalService.listarTodosPorUsuario(anyString())).thenReturn(List.of(orcamento));
            when(orcamentoMensalMapper.mapTo(any(OrcamentoMensalEntity.class))).thenReturn(orcamentoDTO);

            mockMvc.perform(get("/orcamento-mensal")
                    .header("Authorization", "Bearer token_valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].valorLimite").value(1000));
        }

        @Test
        void deveListarOrcamentosPorPeriodo() throws Exception {
            YearMonth periodo = YearMonth.now();
            when(jwtUtil.extractUserId(anyString())).thenReturn(user.getUuid());
            when(orcamentoMensalService.listarPorPeriodo(anyString(), any(YearMonth.class))).thenReturn(List.of(orcamento));
            when(orcamentoMensalMapper.mapTo(any(OrcamentoMensalEntity.class))).thenReturn(orcamentoDTO);

            mockMvc.perform(get("/orcamento-mensal/periodo/" + periodo)
                    .header("Authorization", "Bearer token_valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].periodo").exists());
        }
    }

    @Nested
    class BuscarOrcamentoPorIdTest {
        @Test
        void deveBuscarOrcamentoPorId() throws Exception {
            when(jwtUtil.extractUserId(anyString())).thenReturn(user.getUuid());
            when(orcamentoMensalService.buscarPorId(user.getUuid(), orcamento.getUuid())).thenReturn(orcamento);
            when(orcamentoMensalMapper.mapTo(any(OrcamentoMensalEntity.class))).thenReturn(orcamentoDTO);

            mockMvc.perform(get("/orcamento-mensal/" + orcamento.getUuid())
                    .header("Authorization", "Bearer token_valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorLimite").value(1000));
        }
    }

    @Nested
    class CriarOrcamentoTest {
        @Test
        void deveCriarOrcamento() throws Exception {
            OrcamentoMensalDTO requestDTO = new OrcamentoMensalDTO();
            requestDTO.setCategoria("ALIMENTACAO");
            requestDTO.setValorLimite(BigDecimal.valueOf(500));
            requestDTO.setPeriodo(YearMonth.now());

            when(jwtUtil.extractUserId(anyString())).thenReturn(user.getUuid());
            when(orcamentoMensalService.criarOrcamentoMensal(
                anyString(), 
                eq("ALIMENTACAO"), 
                any(BigDecimal.class), 
                any(YearMonth.class))
            ).thenReturn(orcamento);
            when(orcamentoMensalMapper.mapTo(any(OrcamentoMensalEntity.class))).thenReturn(orcamentoDTO);

            mockMvc.perform(post("/orcamento-mensal")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDTO))
                    .header("Authorization", "Bearer token_valido"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
        }

        @Test
        void deveRetornarBadRequestParaCriacaoInvalida() throws Exception {
            OrcamentoMensalDTO requestDTO = new OrcamentoMensalDTO();
            
            mockMvc.perform(post("/orcamento-mensal")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDTO))
                    .header("Authorization", "Bearer token_valido"))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class AtualizarOrcamentoTest {
        @Test
        void deveAtualizarOrcamento() throws Exception {
            OrcamentoMensalRequest request = new OrcamentoMensalRequest(
                "TRANSPORTE", 
                BigDecimal.valueOf(600), 
                YearMonth.now()
            );

            when(jwtUtil.extractUserId(anyString())).thenReturn(user.getUuid());
            when(orcamentoMensalService.atualizarOrcamentoMensal(
                user.getUuid(), 
                orcamento.getUuid(), 
                "TRANSPORTE", 
                BigDecimal.valueOf(600), 
                YearMonth.now())
            ).thenReturn(orcamento);
            when(orcamentoMensalMapper.mapTo(any(OrcamentoMensalEntity.class))).thenReturn(orcamentoDTO);

            mockMvc.perform(put("/orcamento-mensal/" + orcamento.getUuid())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .header("Authorization", "Bearer token_valido"))
                .andExpect(status().isOk());
        }
    }

    @Nested
    class ExcluirOrcamentoTest {
        @Test
        void deveExcluirOrcamento() throws Exception {
            when(jwtUtil.extractUserId(anyString())).thenReturn(user.getUuid());
            doNothing().when(orcamentoMensalService).excluirOrcamentoMensal(user.getUuid(), orcamento.getUuid());
            
            mockMvc.perform(delete("/orcamento-mensal/" + orcamento.getUuid())
                    .header("Authorization", "Bearer token_valido"))
                .andExpect(status().isNoContent());
        }
    }

    @Test
    void deveExtrairTokenCorretamente() throws Exception {
        OrcamentoMensalDTO requestDTO = new OrcamentoMensalDTO();
        requestDTO.setCategoria("TRANSPORTE");
        requestDTO.setValorLimite(BigDecimal.valueOf(500));
        requestDTO.setPeriodo(YearMonth.now());

        String expectedToken = "token_esperado";
        when(jwtUtil.extractUserId(expectedToken)).thenReturn(user.getUuid());
        when(orcamentoMensalService.criarOrcamentoMensal(anyString(), anyString(), any(BigDecimal.class), any(YearMonth.class)))
            .thenReturn(orcamento);
        when(orcamentoMensalMapper.mapTo(any(OrcamentoMensalEntity.class))).thenReturn(orcamentoDTO);

        mockMvc.perform(post("/orcamento-mensal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO))
                .header("Authorization", "Bearer " + expectedToken));

        verify(jwtUtil).extractUserId(expectedToken);
    }

    @Nested
    class ValidacaoDadosTest {
        @Test
        void criarOrcamento_DeveRetornarBadRequest_QuandoValorInvalido() throws Exception {
            OrcamentoMensalDTO dtoInvalido = new OrcamentoMensalDTO();
            dtoInvalido.setCategoria("ALIMENTACAO");
            dtoInvalido.setValorLimite(BigDecimal.ZERO); // Valor inválido
            dtoInvalido.setPeriodo(YearMonth.now());
            
            mockMvc.perform(post("/orcamento-mensal")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dtoInvalido))
                    .header("Authorization", "Bearer token_valido"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void atualizarOrcamento_DeveRetornarBadRequest_QuandoCategoriaVazia() throws Exception {
            OrcamentoMensalRequest requestInvalido = new OrcamentoMensalRequest(
                "", // Categoria vazia
                BigDecimal.valueOf(100),
                YearMonth.now()
            );
            
            mockMvc.perform(put("/orcamento-mensal/" + orcamento.getUuid())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestInvalido))
                    .header("Authorization", "Bearer token_valido"))
                .andExpect(status().isBadRequest());
        }
    }
}
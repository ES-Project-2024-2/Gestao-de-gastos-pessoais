package br.com.gestorfinanceiro.controller;

import br.com.gestorfinanceiro.config.security.JwtUtil;
import br.com.gestorfinanceiro.dto.dashboard.SaldoTotalDTO;
import br.com.gestorfinanceiro.dto.despesa.DespesaDTO;
import br.com.gestorfinanceiro.dto.receita.ReceitaDTO;
import br.com.gestorfinanceiro.mappers.Mapper;
import br.com.gestorfinanceiro.models.DespesaEntity;
import br.com.gestorfinanceiro.models.ReceitaEntity;
import br.com.gestorfinanceiro.services.DashboardService;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final Mapper<DespesaEntity, DespesaDTO> despesaMapper;
    private final Mapper<ReceitaEntity, ReceitaDTO> receitaMapper;
    private final JwtUtil jwtUtil;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    public DashboardController(DashboardService dashboardService,
                               Mapper<DespesaEntity, DespesaDTO> despesaMapper,
                               Mapper<ReceitaEntity, ReceitaDTO> receitaMapper,
                               JwtUtil jwtUtil) {
        this.dashboardService = dashboardService;
        this.despesaMapper = despesaMapper;
        this.receitaMapper = receitaMapper;
        this.jwtUtil = jwtUtil;
    }

    private String getUserIdFromToken(HttpServletRequest request) {
        String token = request.getHeader(AUTHORIZATION_HEADER).replace(BEARER_PREFIX, "");
        return jwtUtil.extractUserId(token);
    }

    @GetMapping("/saldo-total")
    public ResponseEntity<SaldoTotalDTO> getSaldoTotal(@RequestParam YearMonth periodo,
                                                       HttpServletRequest request) {
        String userId = getUserIdFromToken(request);
        BigDecimal saldo = dashboardService.getSaldoTotal(userId, periodo);

        SaldoTotalDTO response = new SaldoTotalDTO(periodo, saldo);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/maior-despesa")
    public ResponseEntity<DespesaDTO> getMaiorDespesa(@RequestParam YearMonth periodo,
                                                    HttpServletRequest request) {
        try {
            String userId = getUserIdFromToken(request);
            DespesaEntity despesa = dashboardService.getMaiorDespesa(userId, periodo);
            
            if (despesa == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(despesaMapper.mapTo(despesa));
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/maior-receita")
    public ResponseEntity<ReceitaDTO> getMaiorReceita(@RequestParam YearMonth periodo,
                                                      HttpServletRequest request) {
        try {
            String userId = getUserIdFromToken(request);
            ReceitaEntity receita = dashboardService.getMaiorReceita(userId, periodo);
            
            if (receita == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(receitaMapper.mapTo(receita));
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/categoria-maior-despesa")
    public ResponseEntity<Map<String, BigDecimal>> getCategoriaComMaiorDespesa(
            @RequestParam YearMonth periodo,
            HttpServletRequest request) {
        String userId = getUserIdFromToken(request);
        return ResponseEntity.ok(dashboardService.getCategoriaComMaiorDespesa(userId, periodo));
    }

    @GetMapping("/categoria-maior-receita")
    public ResponseEntity<Map<String, BigDecimal>> getCategoriaComMaiorReceita(
            @RequestParam YearMonth periodo,
            HttpServletRequest request) {
        String userId = getUserIdFromToken(request);
        return ResponseEntity.ok(dashboardService.getCategoriaComMaiorReceita(userId, periodo));
    }

    @GetMapping("/despesa-total")
    public ResponseEntity<SaldoTotalDTO> getTotalDespesasNoMes(@RequestParam YearMonth periodo,
                                                      HttpServletRequest request) {
        String userId = getUserIdFromToken(request);
        BigDecimal despesaTotal = dashboardService.calcularTotalDespesasNoMes(userId, periodo);
        SaldoTotalDTO response = new SaldoTotalDTO(periodo, despesaTotal);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/receita-total")
    public ResponseEntity<SaldoTotalDTO> getTotalReceitasNoMes(@RequestParam YearMonth periodo,
                                                      HttpServletRequest request) {
        String userId = getUserIdFromToken(request);
        BigDecimal receitaTotal = dashboardService.calcularTotalReceitasNoMes(userId, periodo);
        SaldoTotalDTO response = new SaldoTotalDTO(periodo, receitaTotal);

        return ResponseEntity.ok(response);
    }
}
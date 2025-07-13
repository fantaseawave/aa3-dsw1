package com.gametester.service;

import com.gametester.model.SessaoTeste;
import com.gametester.repository.SessaoTesteRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SessionSchedulerService {

    private final SessaoTesteRepository sessaoTesteRepository;

    public SessionSchedulerService(SessaoTesteRepository sessaoTesteRepository) {
        this.sessaoTesteRepository = sessaoTesteRepository;
    }

    @Scheduled(fixedRate = 60000)
    public void autoFinalizarSessoes() {
        List<SessaoTeste> sessoesEmExecucao = sessaoTesteRepository.findAll()
                .stream()
                .filter(s -> "EM_EXECUCAO".equals(s.getStatus()))
                .toList();

        for (SessaoTeste sessao : sessoesEmExecucao) {
            long inicioMillis = sessao.getDataHoraInicio().getTime();
            long duracaoMillis = TimeUnit.MINUTES.toMillis(sessao.getTempoSessaoMinutos());
            long tempoLimiteMillis = inicioMillis + duracaoMillis;

            if (System.currentTimeMillis() >= tempoLimiteMillis) {
                sessao.setStatus("FINALIZADO");
                sessao.setDataHoraFim(new Timestamp(System.currentTimeMillis()));
                sessaoTesteRepository.save(sessao);
            }
        }
    }
}
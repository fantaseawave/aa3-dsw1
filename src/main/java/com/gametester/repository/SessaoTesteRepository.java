package com.gametester.repository;

import com.gametester.model.SessaoTeste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessaoTesteRepository extends JpaRepository<SessaoTeste, Integer> {

    List<SessaoTeste> findByTestadorId(int testadorId);

    List<SessaoTeste> findByProjetoId(int projetoId);
}
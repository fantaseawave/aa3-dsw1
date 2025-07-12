package com.gametester.repository;

import com.gametester.model.Bug;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BugRepository extends JpaRepository<Bug, Integer> {

    List<Bug> findBySessaoTesteId(int sessaoTesteId);
}
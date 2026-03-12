package com.childcare.domain.checklist.repository;

import com.childcare.domain.checklist.entity.ChildGrowChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChildGrowChecklistRepository extends JpaRepository<ChildGrowChecklist, Long> {
}

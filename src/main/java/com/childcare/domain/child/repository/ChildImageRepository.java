package com.childcare.domain.child.repository;

import com.childcare.domain.child.entity.ChildImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChildImageRepository extends JpaRepository<ChildImage, Long> {

    List<ChildImage> findAllByChSeq(Long chSeq);

    void deleteByChSeq(Long chSeq);
}

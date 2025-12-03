package com.childcare.domain.child.mapper;

import com.childcare.domain.child.entity.Child;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ChildMapper {

    List<Child> findActiveChildrenByMemberSeq(@Param("memberSeq") Long memberSeq);

    Optional<Child> findActiveChildById(@Param("childId") Long childId);
}

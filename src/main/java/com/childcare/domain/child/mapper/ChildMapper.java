package com.childcare.domain.child.mapper;

import com.childcare.domain.child.entity.Child;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChildMapper {

    List<Child> findActiveChildrenByMemberSeq(@Param("memberSeq") Long memberSeq);
}

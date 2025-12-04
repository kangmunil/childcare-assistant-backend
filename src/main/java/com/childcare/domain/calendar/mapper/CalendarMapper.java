package com.childcare.domain.calendar.mapper;

import com.childcare.domain.calendar.entity.Calendar;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CalendarMapper {

    List<Calendar> findCalendarsByChildId(@Param("childId") Long childId);

    List<Calendar> findCalendarsByChildIdAndDate(@Param("childId") Long childId, @Param("caDate") String caDate);

    List<Calendar> findCalendarsByChildIdAndMonth(@Param("childId") Long childId, @Param("yearMonth") String yearMonth);

    Optional<Calendar> findActiveCalendarById(@Param("childId") Long childId, @Param("caSeq") Long caSeq);
}

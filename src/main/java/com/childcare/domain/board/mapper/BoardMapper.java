package com.childcare.domain.board.mapper;

import com.childcare.domain.board.dto.BoardCommentListDto;
import com.childcare.domain.board.dto.BoardItemListDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface BoardMapper {

        // 게시글 검색
        List<BoardItemListDto> searchItems(
                        @Param("boardId") Long boardId,
                        @Param("regionCode") String regionCode,
                        @Param("postcode") Integer postcode,
                        @Param("postcodePrefix") Integer postcodePrefix,
                        @Param("category") String category,
                        @Param("memberId") UUID memberId,
                        @Param("searchType") String searchType,
                        @Param("keyword") String keyword,
                        @Param("locationScope") String locationScope,
                        @Param("offset") int offset,
                        @Param("limit") int limit);

        // 게시글 검색 총 개수
        int countSearchItems(
                        @Param("boardId") Long boardId,
                        @Param("regionCode") String regionCode,
                        @Param("postcode") Integer postcode,
                        @Param("postcodePrefix") Integer postcodePrefix,
                        @Param("category") String category,
                        @Param("searchType") String searchType,
                        @Param("keyword") String keyword,
                        @Param("locationScope") String locationScope);

        // 댓글 목록 조회 (정렬: 고정댓글 우선 > 부모-자식 그룹핑 > 등록일순)
        List<BoardCommentListDto> getComments(
                        @Param("itemId") Long itemId,
                        @Param("memberId") UUID memberId);

        // 고정글 조회
        List<BoardItemListDto> getFixedItems(
                        @Param("boardId") Long boardId,
                        @Param("regionCode") String regionCode,
                        @Param("postcode") Integer postcode,
                        @Param("postcodePrefix") Integer postcodePrefix,
                        @Param("category") String category,
                        @Param("locationScope") String locationScope,
                        @Param("memberId") UUID memberId);

        // 인기글 조회 (조회수+공감수 상위 3건, 고정글 제외)
        List<BoardItemListDto> getPopularItems(
                        @Param("boardId") Long boardId,
                        @Param("regionCode") String regionCode,
                        @Param("postcode") Integer postcode,
                        @Param("postcodePrefix") Integer postcodePrefix,
                        @Param("category") String category,
                        @Param("locationScope") String locationScope,
                        @Param("memberId") UUID memberId);

        // 전체 게시글 조회 (게시판 구분 없이)
        List<BoardItemListDto> searchAllItems(
                        @Param("memberId") UUID memberId,
                        @Param("postcode") Integer postcode,
                        @Param("searchType") String searchType,
                        @Param("keyword") String keyword,
                        @Param("offset") int offset,
                        @Param("limit") int limit);

        int countAllItems(
                        @Param("postcode") Integer postcode,
                        @Param("searchType") String searchType,
                        @Param("keyword") String keyword);
}

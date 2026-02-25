package com.childcare.domain.board.service;

import com.childcare.domain.board.dto.BoardDto;
import com.childcare.domain.board.dto.BoardItemListDto;
import com.childcare.domain.board.dto.BoardSearchRequest;
import com.childcare.domain.board.mapper.BoardMapper;
import com.childcare.domain.board.repository.BoardRepository;
import com.childcare.domain.member.repository.MemberRepository;
import com.childcare.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardMapper boardMapper;
    private final MemberRepository memberRepository;

    public ApiResponse<List<BoardDto>> getBoards() {
        List<BoardDto> boards = boardRepository.findByBoUseYnAndBoReadAuth("Y", "USER")
                .stream()
                .map(board -> BoardDto.builder()
                        .id(board.getBoSeq())
                        .code(board.getBoCode())
                        .slug(board.getBoSlug())
                        .title(board.getBoTitle())
                        .description(board.getBoDesc())
                        .type(board.getBoType())
                        .neighborYn(board.getBoNeighborYn())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.success("게시판 목록 조회 성공", boards);
    }

    public ApiResponse<Map<String, Object>> getAllItems(UUID memberId, BoardSearchRequest searchRequest) {
        int page = searchRequest.getPage();
        int size = searchRequest.getSize();
        int offset = page * size;

        Integer postcode = memberRepository.findById(memberId)
                .map(member -> {
                    try { return Integer.parseInt(member.getPostcode()); }
                    catch (Exception e) { return null; }
                })
                .orElse(null);

        List<BoardItemListDto> items = boardMapper.searchAllItems(
                memberId, postcode, searchRequest.getSearchType(), searchRequest.getKeyword(), offset, size);

        int totalCount = boardMapper.countAllItems(postcode, searchRequest.getSearchType(), searchRequest.getKeyword());
        int totalPages = (int) Math.ceil((double) totalCount / size);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("currentPage", page);
        result.put("totalPages", totalPages);
        result.put("totalCount", totalCount);

        return ApiResponse.success("전체 게시글 조회 성공", result);
    }
}

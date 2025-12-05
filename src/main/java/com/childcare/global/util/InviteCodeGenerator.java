package com.childcare.global.util;

import com.childcare.domain.member.repository.MemberRepository;
import com.childcare.global.exception.AuthException;
import com.childcare.global.exception.AuthException.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class InviteCodeGenerator {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 7;
    private static final int MAX_RETRY = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final MemberRepository memberRepository;

    /**
     * 7자리 랜덤 초대코드 생성 (중복 시 재시도)
     */
    public String generate() {
        for (int i = 0; i < MAX_RETRY; i++) {
            String code = generateRandomCode();
            if (!memberRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new AuthException(AuthErrorCode.INVITE_CODE_GENERATION_FAILED);
    }

    private String generateRandomCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }
}

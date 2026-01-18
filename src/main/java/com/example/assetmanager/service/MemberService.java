package com.example.assetmanager.service;

import com.example.assetmanager.domain.Member;
import com.example.assetmanager.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional
    public Member signup(String loginId, String password, String spreadsheetId) {
        if (memberRepository.findByLoginId(loginId).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }
        Member member = new Member(loginId, password, spreadsheetId);
        return memberRepository.save(member);
    }

    public Optional<Member> login(String loginId, String password) {
        return memberRepository.findByLoginId(loginId)
                .filter(m -> m.getPassword().equals(password)); // 간단한 평문 비교 (개발 단계)
    }

    @Transactional
    public void withdraw(Long memberId) {
        memberRepository.deleteById(memberId);
    }

    public Optional<Member> findById(Long id) {
        return memberRepository.findById(id);
    }
}

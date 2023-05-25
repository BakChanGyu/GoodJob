package com.goodjob.domain.member.service;

import com.goodjob.domain.member.dto.request.JoinRequestDto;
import com.goodjob.domain.member.entity.Member;
import com.goodjob.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;

    public boolean canJoin(JoinRequestDto joinRequestDto) {
        Optional<Member> opEmail = findByEmail(joinRequestDto.getEmail());
        Optional<Member> opAccount = findByAccount(joinRequestDto.getAccount());

        if (opEmail.isPresent()) {
            return false;
        }

        if (opAccount.isPresent()) {
            return false;
        }

        return true;
    }

    @Transactional
    public Member join(JoinRequestDto joinRequestDto) {
        String password = passwordEncoder.encode(joinRequestDto.getPassword());

        Member member = Member
                .builder()
                .account(joinRequestDto.getAccount())
                .password(password)
                .username(joinRequestDto.getUsername())
                .email(joinRequestDto.getEmail())
                .phone(joinRequestDto.getPhone())
                .isDeleted(false)
                .build();

        return memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public Optional<Member> findByUsername(String username) {
        return memberRepository.findByUsername(username);
    }
}

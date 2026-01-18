package com.example.assetmanager.auth;

import com.example.assetmanager.domain.Member;
import com.example.assetmanager.service.MemberService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> request) {
        try {
            String loginId = request.get("loginId");
            String password = request.get("password");
            String spreadsheetId = request.get("spreadsheetId");

            Member member = memberService.signup(loginId, password, spreadsheetId);
            return ResponseEntity.ok(Map.of("message", "Signup successful", "loginId", member.getLoginId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request, HttpSession session) {
        String loginId = request.get("loginId");
        String password = request.get("password");

        Optional<Member> memberOpt = memberService.login(loginId, password);

        if (memberOpt.isPresent()) {
            Member member = memberOpt.get();
            session.setAttribute("authenticated", true);
            session.setAttribute("memberId", member.getId());
            session.setMaxInactiveInterval(60 * 60 * 24 * 7);
            return ResponseEntity.ok(Map.of("message", "Login successful", "loginId", member.getLoginId()));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid ID or password"));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> status(HttpSession session) {
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        Long memberId = (Long) session.getAttribute("memberId");

        if (authenticated != null && authenticated && memberId != null) {
            return memberService.findById(memberId)
                    .map(member -> ResponseEntity.ok(Map.of(
                            "authenticated", true,
                            "loginId", member.getLoginId(),
                            "spreadsheetId", member.getSpreadsheetId())))
                    .orElse(ResponseEntity.ok(Map.of("authenticated", false)));
        } else {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(HttpSession session) {
        Long memberId = (Long) session.getAttribute("memberId");
        if (memberId != null) {
            memberService.withdraw(memberId);
            session.invalidate();
            return ResponseEntity.ok(Map.of("message", "Account withdrawn successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
    }
}

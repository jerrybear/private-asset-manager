package com.example.assetmanager.auth;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${APP_AUTH_PASSCODE:1234}")
    private String passcode;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request, HttpSession session) {
        String inputPasscode = request.get("passcode");

        if (passcode.equals(inputPasscode)) {
            session.setAttribute("authenticated", true);
            // 세션 유지 시간을 7일 정도로 설정 (초 단위: 60 * 60 * 24 * 7)
            session.setMaxInactiveInterval(60 * 60 * 24 * 7);
            return ResponseEntity.ok(Map.of("message", "Login successful"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid passcode"));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> status(HttpSession session) {
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        if (authenticated != null && authenticated) {
            return ResponseEntity.ok(Map.of("authenticated", true));
        } else {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}

package com.lwe.api;

import com.lwe.api.dto.UserInfoResponse;
import com.lwe.core.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserSearchController {

    private final UserRepository userRepo;

    public UserSearchController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserInfoResponse>> search(@RequestParam String q) {
        if (q.length() < 3) return ResponseEntity.ok(List.of());
        var results = userRepo.findByEmailContainingIgnoreCaseOrUsernameContainingIgnoreCase(q, q);
        return ResponseEntity.ok(results.stream()
            .map(u -> new UserInfoResponse(u.getId(), u.getEmail(), u.getUsername(), u.getRole(), u.getLocale()))
            .toList());
    }
}

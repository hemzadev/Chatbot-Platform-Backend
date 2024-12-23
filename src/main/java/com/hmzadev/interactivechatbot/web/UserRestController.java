package com.hmzadev.interactivechatbot.web;

import com.hmzadev.interactivechatbot.dao.User;
import com.hmzadev.interactivechatbot.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserRestController {

    @Autowired
    private UserService userService;

    // Endpoint to create or update a user
    @PostMapping("/add")
    public ResponseEntity<String> addUser(@RequestParam String username, @RequestParam String password) {
        try {
            Optional<User> existingUserOpt = userService.findByEmail(username);  // Directly use the result
            if (existingUserOpt.isPresent()) {
                return ResponseEntity.badRequest().body("User already exists");
            }

            User newUser = User.builder()
                    .username(username)
                    .password(password)  // Hash the password before saving in a real application
                    .build();
            userService.save(newUser);

            return ResponseEntity.ok("User created successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }


    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        try {
            Optional<User> userOpt = userService.findByEmail(email);
            return userOpt.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        try {
            Optional<User> userOpt = userService.findByUsername(username);
            return userOpt.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
}

package com.desapp.futbolplayerstokens.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    private String password;

    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;

    public enum Role {
        USER, ADMIN, SUPERUSER
    }

    @Column(precision = 19, scale = 8)
    @Builder.Default
    private java.math.BigDecimal balance = new java.math.BigDecimal("1000");
}
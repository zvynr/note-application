package com.example.note.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** Role: USER or ADMIN */
    @Column(nullable = false)
    private String role = "USER";

    /** Email ownership verified via link */
    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(name = "verification_token")
    @JsonIgnore
    private String verificationToken;

    @Column(name = "verification_token_expiry")
    @JsonIgnore
    private Date verificationTokenExpiry;

    private Long quantity;

    private String profilePath;

    // Reset password token
    @Column(name = "reset_token")
    @JsonIgnore
    private String resetToken;

    // Token expiry date
    @Column(name = "token_expiry_date")
    @JsonIgnore
    private Date tokenExpiryDate;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Note> notes;

    @PostLoad
    @PostUpdate
    private void updateQuantity() {
        this.quantity = calculateQuantity();
    }

    private Long calculateQuantity() {
        if (notes != null) {
            return (long) notes.size();
        }
        return 0L;
    }
}

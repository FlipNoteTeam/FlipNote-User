package flipnote.user.domain.user.domain;

import flipnote.user.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String nickname;

    private String profileImageUrl;

    @Column(unique = true, length = 20)
    private String phone;

    private boolean smsAgree;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    private LocalDateTime invalidatedAt;

    private LocalDateTime deletedAt;

    @Builder
    public User(String email, String password, String name, String nickname, String phone, boolean smsAgree, Role role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.nickname = nickname;
        this.phone = phone;
        this.smsAgree = smsAgree;
        this.role = role != null ? role : Role.USER;
        this.status = Status.ACTIVE;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
        this.invalidatedAt = LocalDateTime.now();
    }

    public void updateProfile(String nickname, String phone, boolean smsAgree, String profileImageUrl) {
        this.nickname = nickname;
        this.phone = phone;
        this.smsAgree = smsAgree;
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
    }

    public void withdraw() {
        this.status = Status.WITHDRAWN;
        this.invalidatedAt = LocalDateTime.now();
        this.deletedAt = LocalDateTime.now();
    }

    public enum Role {
        USER, ADMIN
    }

    public enum Status {
        ACTIVE, WITHDRAWN
    }
}

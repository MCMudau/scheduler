package com.mphoYanga.scheduler.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phoneNumber;

    private String address;

    @Column(nullable = false)
    private String password;

    /** 5-digit OTP sent to email for verification */
    private String verificationPin;

    private LocalDateTime pinExpiresAt;

    private boolean verified = false;

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ── Getters & Setters ─────────────────────────────────────────────

    public Long getId()                          { return id; }

    public String getName()                      { return name; }
    public void setName(String name)             { this.name = name; }

    public String getSurname()                   { return surname; }
    public void setSurname(String surname)       { this.surname = surname; }

    public String getEmail()                     { return email; }
    public void setEmail(String email)           { this.email = email; }

    public String getPhoneNumber()               { return phoneNumber; }
    public void setPhoneNumber(String p)         { this.phoneNumber = p; }

    public String getAddress()                   { return address; }
    public void setAddress(String address)       { this.address = address; }

    public String getPassword()                  { return password; }
    public void setPassword(String password)     { this.password = password; }

    public String getVerificationPin()           { return verificationPin; }
    public void setVerificationPin(String pin)   { this.verificationPin = pin; }

    public LocalDateTime getPinExpiresAt()       { return pinExpiresAt; }
    public void setPinExpiresAt(LocalDateTime t) { this.pinExpiresAt = t; }

    public boolean isVerified()                  { return verified; }
    public void setVerified(boolean verified)    { this.verified = verified; }

    public LocalDateTime getCreatedAt()          { return createdAt; }

    public Boolean getVerified() {
        return verified;
    }
}
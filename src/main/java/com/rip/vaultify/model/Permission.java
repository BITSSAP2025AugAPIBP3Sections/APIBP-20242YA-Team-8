package com.rip.vaultify.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "permissions")
@Getter
@Setter
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    private FileEntity file;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Access access;

    public enum Access{
        READ, WRITE, OWNER
    }
}
package com.officearcade.server.store.persistence;

import com.officearcade.server.store.CosmeticCategory;
import com.officearcade.server.users.persistence.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_equipped_cosmetics")
public class UserEquippedCosmeticEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cosmetic_item_id", nullable = false)
    private CosmeticItemEntity cosmeticItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private CosmeticCategory category;

    @Column(name = "equipped_at", nullable = false)
    private Instant equippedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (equippedAt == null) {
            equippedAt = Instant.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        equippedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public CosmeticItemEntity getCosmeticItem() {
        return cosmeticItem;
    }

    public void setCosmeticItem(CosmeticItemEntity cosmeticItem) {
        this.cosmeticItem = cosmeticItem;
    }

    public CosmeticCategory getCategory() {
        return category;
    }

    public void setCategory(CosmeticCategory category) {
        this.category = category;
    }

    public Instant getEquippedAt() {
        return equippedAt;
    }

    public void setEquippedAt(Instant equippedAt) {
        this.equippedAt = equippedAt;
    }
}

package com.officearcade.server.store.persistence;

import com.officearcade.server.store.CosmeticCategory;
import com.officearcade.server.store.CosmeticRarity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cosmetic_items")
public class CosmeticItemEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 80)
    private String code;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "description", nullable = false, length = 240)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private CosmeticCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "rarity", nullable = false, length = 16)
    private CosmeticRarity rarity;

    @Column(name = "price_respect", nullable = false)
    private int priceRespect;

    @Column(name = "preview_asset_key", nullable = false, length = 120)
    private String previewAssetKey;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CosmeticCategory getCategory() {
        return category;
    }

    public void setCategory(CosmeticCategory category) {
        this.category = category;
    }

    public CosmeticRarity getRarity() {
        return rarity;
    }

    public void setRarity(CosmeticRarity rarity) {
        this.rarity = rarity;
    }

    public int getPriceRespect() {
        return priceRespect;
    }

    public void setPriceRespect(int priceRespect) {
        this.priceRespect = priceRespect;
    }

    public String getPreviewAssetKey() {
        return previewAssetKey;
    }

    public void setPreviewAssetKey(String previewAssetKey) {
        this.previewAssetKey = previewAssetKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

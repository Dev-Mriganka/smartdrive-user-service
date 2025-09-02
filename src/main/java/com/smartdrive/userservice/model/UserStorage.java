package com.smartdrive.userservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * UserStorage entity for tracking file storage usage and quotas
 */
@Entity
@Table(name = "user_storage")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStorage {

    @Id
    @Column(name = "user_id")
    private UUID userId; // References user_profiles.id

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserProfile user;

    @Column(name = "storage_used")
    @Builder.Default
    private Long storageUsed = 0L; // bytes

    @Column(name = "storage_limit")
    @Builder.Default
    private Long storageLimit = 5368709120L; // 5GB default

    @Column(name = "files_count")
    @Builder.Default
    private Integer filesCount = 0;

    @Column(name = "last_calculated_at")
    @Builder.Default
    private LocalDateTime lastCalculatedAt = LocalDateTime.now();

    // Helper methods
    public boolean hasStorageAvailable(long requiredBytes) {
        return (storageUsed + requiredBytes) <= storageLimit;
    }

    public long getAvailableStorage() {
        return storageLimit - storageUsed;
    }

    public double getStorageUsagePercentage() {
        return storageLimit > 0 ? (double) storageUsed / storageLimit * 100 : 0;
    }

    public void addStorageUsage(long bytes) {
        this.storageUsed += bytes;
        this.filesCount++;
        this.lastCalculatedAt = LocalDateTime.now();
    }

    public void removeStorageUsage(long bytes) {
        this.storageUsed = Math.max(0, this.storageUsed - bytes);
        this.filesCount = Math.max(0, this.filesCount - 1);
        this.lastCalculatedAt = LocalDateTime.now();
    }
}

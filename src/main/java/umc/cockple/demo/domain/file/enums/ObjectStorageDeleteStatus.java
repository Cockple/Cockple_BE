package umc.cockple.demo.domain.file.enums;

import java.util.List;

public enum ObjectStorageDeleteStatus {
    PENDING,
    PROCESSING,
    DONE,
    FAILED;

    public static List<ObjectStorageDeleteStatus> retryableStatuses() {
        return List.of(PENDING, FAILED);
    }
}

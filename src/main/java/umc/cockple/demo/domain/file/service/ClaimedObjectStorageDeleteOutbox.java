package umc.cockple.demo.domain.file.service;

record ClaimedObjectStorageDeleteOutbox(
        Long id,
        String objectKey,
        String claimToken
) {
}

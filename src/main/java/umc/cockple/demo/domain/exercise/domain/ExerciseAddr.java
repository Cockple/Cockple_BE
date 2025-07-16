package umc.cockple.demo.domain.exercise.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.exercise.dto.ExerciseAddrCreateCommand;
import umc.cockple.demo.domain.exercise.dto.ExerciseAddrUpdateCommand;
import umc.cockple.demo.global.common.BaseEntity;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class ExerciseAddr extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String addr1; // 도, 광역시

    @Column(nullable = false)
    private String addr2; // 시군구

    @Column(nullable = false)
    private String streetAddr;

    @Column(nullable = false)
    private String buildingName;

    @Column(nullable = false)
    private Float latitude;

    @Column(nullable = false)
    private Float longitude;

    public static ExerciseAddr create(ExerciseAddrCreateCommand command) {
        AddressParts addressParts = parseRoadAddress(command.roadAddress());

        return ExerciseAddr.builder()
                .addr1(addressParts.addr1())
                .addr2(addressParts.addr2())
                .streetAddr(command.roadAddress())
                .buildingName(command.buildingName())
                .latitude(command.latitude().floatValue())
                .longitude(command.longitude().floatValue())
                .build();
    }

    public void updateAddress(ExerciseAddrUpdateCommand command) {
        if (command.roadAddress() != null && !command.roadAddress().isEmpty()) {
            AddressParts addressParts = parseRoadAddress(command.roadAddress());
            this.addr1 = addressParts.addr1();
            this.addr2 = addressParts.addr2();
            this.streetAddr = command.roadAddress();
        }

        if (command.buildingName() != null) {
            this.buildingName = command.buildingName();
        }

        if (command.latitude() != null) {
            this.latitude = command.latitude().floatValue();
        }

        if (command.longitude() != null) {
            this.longitude = command.longitude().floatValue();
        }
    }

    private static AddressParts parseRoadAddress(String roadAddress) {
        String[] parts = roadAddress.trim().split("\\s+");

        String addr1 = extractAddr1(parts);
        String addr2 = extractAddr2(parts);

        return new AddressParts(addr1, addr2);
    }

    private static String extractAddr1(String[] parts) {
        if (parts.length > 0) {
            String firstPart = parts[0];
            if (firstPart.endsWith("특별시") || firstPart.endsWith("광역시") ||
                    firstPart.endsWith("특별자치시") || firstPart.endsWith("도") ||
                    firstPart.endsWith("특별자치도")) {
                return firstPart;
            }
        }
        return "Unknown";
    }

    private static String extractAddr2(String[] parts) {
        if (parts.length > 1) {
            String secondPart = parts[1];
            if (secondPart.endsWith("시") || secondPart.endsWith("군") || secondPart.endsWith("구")) {
                return secondPart;
            }
        }
        return "Unknown";
    }

    private record AddressParts(
            String addr1,
            String addr2
    ) {
    }

}

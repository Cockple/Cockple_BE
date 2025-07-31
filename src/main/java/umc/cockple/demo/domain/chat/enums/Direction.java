package umc.cockple.demo.domain.chat.enums;

public enum Direction {
    DECS("내림차순"),
    ACS("오름차순");

    private final String description;

    Direction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

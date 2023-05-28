package com.example.examplebatch.part4;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.LocalDate;

@Getter
@Entity
@NoArgsConstructor
public class User {


    @Id
    @GeneratedValue(strategy = javax.persistence.GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(javax.persistence.EnumType.STRING)
    private Level level = Level.NORMAL;

    private int totalAmount;

    private LocalDate updatedDate;

    @Builder
    public User(String name, int totalAmount) {
        this.name = name;
        this.totalAmount = totalAmount;
    }

    public void levelUp() {
        this.level = Level.getNextLevel(this.getTotalAmount());
        this.updatedDate = LocalDate.now();
    }

    public boolean availableLevelUp() {
        return Level.availableLevelUp(this.getLevel(), this.getTotalAmount());
    }
}

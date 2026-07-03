package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.dto.seat.SeatResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// 등급 계산 /가격 계산 등 이뤄지는 로직
@Service
public class SeatGenerator {
    public List<SeatResponse> generateSeats() {
        List<SeatResponse> seats = new ArrayList<>();
        String[] sections = {"A", "B", "C"};
        for (String section : sections) {
            for (char row = 'A'; row <= 'J'; row++) {
                String grade = getSeatGrade(row);
                int price = getSeatPrice(grade);
                for (int seatNum = 1; seatNum <= 10; seatNum++) {

                    SeatResponse seat = SeatResponse.builder()
                            .sectionName(section)
                            .rowNum(String.valueOf(row))
                            .seatNum(seatNum)
                            .seatGrade(grade)
                            .price(price)
                            .status("AVAILABLE")
                            .build();

                    seats.add(seat);
                }
            }
        }
        return seats;
    }
    private String getSeatGrade(char row) {
        if (row == 'A' || row == 'B') {
            return "VIP";
        }
        if (row >= 'C' && row <= 'F') {
            return "R";
        }
        return "S";
    }

    private int getSeatPrice(String grade) {
        return switch (grade) {
            case "VIP" -> 160000;
            case "R" -> 140000;
            default -> 110000;
        };
    }
}

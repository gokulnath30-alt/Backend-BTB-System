package com.busticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingRequest {
    private Long busId;
    private Long scheduleId;
    private List<String> seatIds;
    private List<String> seatNumbers;
    private String paymentMethod;
    
    // Traveler & Route Stop Details
    private String passengerName;
    private String passengerPhone;
    private String passengerEmail;
    private String boardingPoint;
    private String endingPoint;
    private String droppingPoint;
    private String emergencyContact;
}

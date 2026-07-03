package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.UserSession;
import com.clearticket.clearticket.model.dto.ReservationBuyerInfoRequestDto;
import com.clearticket.clearticket.model.dto.ReservationRequestDto;
import com.clearticket.clearticket.model.dto.ReservationResponseDto;
//import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.model.entity.*;
import com.clearticket.clearticket.repository.AddressRepository;
import com.clearticket.clearticket.repository.ReservationRepository;
import com.clearticket.clearticket.repository.ReservationSeatsRepository;
import com.clearticket.clearticket.repository.ScheduleRepository;
import com.clearticket.clearticket.repository.SeatRepository;
import com.clearticket.clearticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final ReservationSeatsRepository reservationSeatsRepository;

    /**
     * 신규 티켓 예매 내역 생성 및 DB저장
     * @param reservationRequestDto 컨트롤러로 부터 전달받은 예약 정보 데이터
     * @return 최종 저장된 예약 데이터
     */
    @Transactional
    public ReservationResponseDto createReservation(ReservationRequestDto reservationRequestDto) {
        User user = userRepository.findById(reservationRequestDto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Schedule schedule = scheduleRepository.findById(reservationRequestDto.getScheduleId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회차(scheduleId)입니다."));

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setSchedule(schedule);
        reservation.setStatus(ReservationStatus.WAITING);
        reservation.setTotalPrice(reservationRequestDto.getTotalPrice());
        reservation.setTicketType(reservationRequestDto.getTicketType());
        reservation.setShippingFee(0);
        reservation.setReservationNumber("RSV-" + System.currentTimeMillis());

        Reservation savedReservation = reservationRepository.save(reservation);

        // 선택한 좌석들을 reservation_seats 에 연결
        List<Long> seatIds = reservationRequestDto.getSeatIds();
        if (seatIds != null && !seatIds.isEmpty()) {
            for (Long seatId : seatIds) {
                Seat seat = seatRepository.findById(seatId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다. ID: " + seatId));

                ReservationSeat reservationSeat = ReservationSeat.builder()
                        .reservation(savedReservation)
                        .seat(seat)
                        .build();

                reservationSeatsRepository.save(reservationSeat);
            }
        }

        return toResponseDto(savedReservation);
    }


    /**
     * 예약 고유 ID로 단건 예약 내역 상세 조회
     * @param reservationId 조회하고자 하는 예약의 고유 ID
     * @return 조회에 성공한 예약 엔티티 데이터
     */
    @Transactional(readOnly = true)
    public ReservationResponseDto getReservationById(Long reservationId) {
        Optional<Reservation> optionalReservation = reservationRepository.findById(reservationId);

        if (optionalReservation.isEmpty()) {
            throw new IllegalArgumentException("해당 예약 내역이 존재하지 않습니다. ID: " + reservationId);
        }

        Reservation reservation = optionalReservation.get();

        return toResponseDto(reservation);
    }


    /**
     * 전체 예약 내역 목록 조회
     * @return 전체 예약 리스트
     */
    @Transactional(readOnly = true)
    public List<ReservationResponseDto> getAllReservations() {
        List<Reservation> reservations = reservationRepository.findAll();
        List<ReservationResponseDto> result = new ArrayList<>();

        for (Reservation reservation : reservations) {
            result.add(toResponseDto(reservation));
        }

        return result;
    }


    /**
     * 예약 고유 ID로 예매 내역 취소 (상태 변경 방식)
     * @param reservationId 취소하고자 하는 예약 고유 ID
     * @throws IllegalArgumentException 존재하지 않는 예약 ID가 들어왔을 경우 예외 발생
     */
    @Transactional
    public void cancelReservationById(Long reservationId) {

        Optional<Reservation> optionalReservation = reservationRepository.findById(reservationId);

        if (optionalReservation.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 예약입니다. ID: " + reservationId);
        }

        Reservation reservation = optionalReservation.get();
        reservation.changeStatus(ReservationStatus.CANCELED);
    }


    /**
     * 예약 엔티티 데이터를 응답 DTO 객체로 변환
     * @param reservation 변환할 예약 엔티티 원본 데이터
     * @return 예약 정보 및 연관 공연 정보가 포함된 최종 응답 DTO 객체
     */
    private ReservationResponseDto toResponseDto(Reservation reservation) {
        Schedule schedule = reservation.getSchedule();
        Performance performance = (schedule != null) ? schedule.getPerformance() : null;

        LocalDateTime perfDateTime = null;
        if (schedule != null && schedule.getShowDate() != null && schedule.getShowTime() != null) {
            perfDateTime = LocalDateTime.of(schedule.getShowDate(), schedule.getShowTime());
        }

        return new ReservationResponseDto(
                reservation.getReservationId(),
                performance != null ? performance.getTitle() : "공연 정보 없음",
                perfDateTime,
                performance != null && performance.getVenue() != null ? performance.getVenue().getName() : "장소 정보 없음",
                null,
                reservation.getTotalPrice(),
                reservation.getStatus() != null ? reservation.getStatus().name() : null
        );
    }


    /**
     * 특정 공연에 설정된 좌석 등급 목록을 중복 없이 조회
     * @param performanceId 조회할 공연의 고유 ID
     * @return 고유한 좌석 등급 리스트
     */
    public List<String> getSeatGrades(Long performanceId) {
        List<String> grades = seatRepository.findDistinctByPerformancePerformanceId(performanceId);

        System.out.println("디버깅: 조회된 등급 리스트 = " + grades);
        return grades;
    }


    /**
     * 예매 상세 페이지에서 사용할 요약 정보(예약 객체, 공연 정보, 좌석 계산 등)를 생성
     * @param reservationId 요약 정보를 생성할 예약 ID
     * @return 화면 렌더링에 필요한 데이터를 담은 Map 객체
     */
    public Map<String, Object> getReservationSummary(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매입니다."));

        Performance performance = (Performance) org.hibernate.Hibernate.unproxy(reservation.getSchedule().getPerformance());
        List<ReservationSeat> resSeats = reservation.getReservationSeats();

        System.out.println("디버깅: 조회된 예약 좌석 수 = " + (resSeats != null ? resSeats.size() : "null"));

        int seatPriceSum = 0;
        Set<String> gradeSet = new LinkedHashSet<>();

        for (ReservationSeat rs : resSeats) {
            gradeSet.add(rs.getSeat().getSeatGrade());
            seatPriceSum += rs.getSeat().getPrice();
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("reservation", reservation);
        summary.put("performance", performance);

        List<Map<String, Object>> selectedGrades = new ArrayList<>();

        for (String grade : gradeSet) {
            Map<String, Object> gradeInfo = new HashMap<>();
            gradeInfo.put("grade", grade); // 예: "R석"

            int count = 0;
            int priceSum = 0;

            for (ReservationSeat rs : resSeats) {
                if (rs.getSeat().getSeatGrade().equals(grade)) {
                    count++;
                    priceSum += rs.getSeat().getPrice();
                }
            }

            gradeInfo.put("count", count);
            gradeInfo.put("price", priceSum);

            selectedGrades.add(gradeInfo);
        }

        summary.put("selectedGrades", selectedGrades);
        summary.put("seatPriceSum", seatPriceSum);

        // 할인 금액 계산 후 맵에 주입
        int discountAmount = 0;
        Coupon usedCoupon = reservation.getUsedCoupon();

        if (usedCoupon != null) {
            String type = String.valueOf(usedCoupon.getDiscountType()); // "AMOUNT" 또는 "PERCENT"
            int val = usedCoupon.getDiscountValue(); // 쿠폰에 지정된 할인 수치

            if ("PERCENT".equals(type)) {
                // 퍼센트 할인 계산
                discountAmount = (int) Math.floor(seatPriceSum * (val / 100.0));
            } else if ("AMOUNT".equals(type)) {
                // 정액 할인
                discountAmount = val;
            }
        }

        summary.put("discountAmount", discountAmount);
        // ==========================================

        return summary;
    }


    /**
     * 예매 1단계 완료 : 선택한 쿠폰 정보를 예매 내역에 임시 저장, 총금액 갱신
     * @param reservationId 현재 진행 중인 예약 ID
     * @param couponId 적용할 쿠폰 ID
     * @param totalPrice 쿠폰이 적용되어 화면에서 계산된 최종 결제 금액
     */
    @Transactional
    public void applyCouponToReservation(Long reservationId, Long couponId, int totalPrice) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매 내역입니다."));

        if (couponId != null) {
            Coupon coupon = new Coupon();
            coupon.setCouponId(couponId);
            reservation.setUsedCoupon(coupon);
        } else {
            reservation.setUsedCoupon(null);
        }

        reservation.setTotalPrice(totalPrice);
    }


    /**
     * 예매 2단계 : 티켓 수령 방식 및 수령인(주문자) 정보 업데이트
     * @param reservationId 수령 정보를 업데이트할 예약 ID
     * @param requestDto 화면에서 넘어온 수령 방식, 배송비, 주문자 정보, 총 금액
     */
    @Transactional
    public void updateBuyerInfo(Long reservationId, ReservationBuyerInfoRequestDto requestDto) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매 내역입니다. ID: " + reservationId));

        reservation.setTicketType(requestDto.getTicketType());
        reservation.setShippingFee(requestDto.getShippingFee());
        reservation.setRecipientName(requestDto.getRecipientName());
        reservation.setRecipientPhone(requestDto.getRecipientPhone());

        String fullAddress = "[" + requestDto.getZonecode() + "] "
                + requestDto.getRoadAddress() + " "
                + requestDto.getDetailAddress();

        reservation.setShippingAddress(fullAddress);

        reservation.setTotalPrice(requestDto.getTotalPrice());
    }

    @Transactional(readOnly = true)
    public Address getDefaultAddressByUserId(Long userId) {

        User user = new User();
        user.setUserId(userId);

        return addressRepository.findByUserAndIsDefaultTrue(user)
                .orElse(null);
    }
}
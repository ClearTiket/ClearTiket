package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.dto.ReservationBuyerInfoRequestDto;
import com.clearticket.clearticket.model.dto.ReservationRequestDto;
import com.clearticket.clearticket.model.dto.ReservationResponseDto;
//import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.model.dto.seat.SeatRequest;
import com.clearticket.clearticket.model.entity.*;
import com.clearticket.clearticket.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final ReservationSeatsRepository reservationSeatsRepository;
    private final BookingSeatsRepository bookingSeatsRepository;
    private final SeatRepository seatRepository;
    private final SeatRedisService seatRedisService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 신규 티켓 예매 내역 생성 및 DB저장
     * @param reservationRequest 컨트롤러로 부터 전달받은 예약 정보 데이터
     * @return 최종 저장된 예약 데이터
     */
    @Transactional
    public ReservationResponseDto createReservation(ReservationRequestDto reservationRequest) {

        User user = userRepository.findById(reservationRequest.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Schedule schedule = scheduleRepository.findById(reservationRequest.getScheduleId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회차(scheduleId)입니다."));

        // ⚠️ 지난 공연(이미 지난 회차)에 대해 프론트엔드의 "예매하기" 버튼이
        // 비활성화되지 않은 채로 남아있어도, 서버에서 한 번 더 최종 방어선으로 막아줍니다.
        LocalDateTime showDateTime = LocalDateTime.of(schedule.getShowDate(), schedule.getShowTime());
        if (showDateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("이미 종료된 회차는 예매할 수 없습니다.");
        }

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setSchedule(schedule);
        reservation.setStatus(ReservationStatus.WAITING);
        reservation.setTotalPrice(reservationRequest.getTotalPrice());
        reservation.setTicketType(reservationRequest.getTicketType());
        reservation.setShippingFee(0);
        reservation.setReservationNumber("RSV-" + System.currentTimeMillis());

        Reservation savedReservation = reservationRepository.save(reservation);

        // 좌석 저장
        if (reservationRequest.getSeats() != null &&
                !reservationRequest.getSeats().isEmpty()) {

            for (SeatRequest seatDto : reservationRequest.getSeats()) {

                ReservationSeat reservationSeat = ReservationSeat.builder()
                        .reservation(savedReservation)
                        .sectionName(seatDto.getSectionName())
                        .rowNum(seatDto.getRowNum())
                        .seatNum(seatDto.getSeatNum())
                        .seatGrade(seatDto.getSeatGrade())
                        .price(seatDto.getPrice())
                        .build();

                reservationSeatsRepository.save(reservationSeat);

                System.out.println("▶ [조회 조건] 스케줄: " + schedule.getScheduleId() + ", 구역: " + seatDto.getSectionName() + ", 열: " + seatDto.getRowNum() + ", 번호: " + seatDto.getSeatNum());

                bookingSeatsRepository.searchByScheduleAndSeat(
                        schedule.getScheduleId(),
                        seatDto.getSectionName(),
                        seatDto.getRowNum(),
                        seatDto.getSeatNum()
                ).ifPresent(bookingSeat -> {
                    System.out.println("▶ [성공] 매칭되는 좌석을 찾았습니다! ID: " + bookingSeat.getBookingSeatId() + ", 원래상태: " + bookingSeat.getStatus());
                    bookingSeat.setStatus(BookingStatus.SELECTED); // 최종 상태는 SELECTED로 변경
                    bookingSeatsRepository.save(bookingSeat);
                });

                // 실시간 매진 알림 및 AI 연동 반영
                Long performanceId = schedule.getPerformance().getPerformanceId();
//                boolean isSoldOut = true; // 로컬 테스트용 고정값
            }

            Map<String, Object> soldOutStatus = checkSectionSoldOutStatus(schedule.getScheduleId());

            if (soldOutStatus != null && "SOLD_OUT".equals(soldOutStatus.get("status"))) {
                String targetSection = (String) soldOutStatus.get("section");

                // 1. 레디스에 매진 기록 (스케줄 ID 기반으로 기록하여 AI 챗봇이 정확히 인지하도록 매핑)
                seatRedisService.markSectionAsSoldOut(schedule.getScheduleId(), targetSection);

                // 3. 브라우저 실시간 전광판으로 신호 쏘기 (★ 핵심: 스케줄 ID 기반 채널로 동적 발송)
                messagingTemplate.convertAndSend("/topic/soldout/" + schedule.getScheduleId(), (Object) soldOutStatus);
            }
        }

        return toResponseDto(savedReservation);
    }

    /**
     * 특정 회차(스케줄)의 구역별 매진 여부를 조회하는 데이터 기반 API 로직
     */
    public Map<String, Object> checkSectionSoldOutStatus(Long scheduleId) {
        Map<String, Object> result = new HashMap<>();

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회차(scheduleId)입니다: " + scheduleId));
        Long performanceId = schedule.getPerformance().getPerformanceId();

        List<BookingSeat> allSeats = bookingSeatsRepository.findByScheduleScheduleId(scheduleId);

        // 1. 구역별로 "SELECTED(예매 확정)" 상태인 좌석 수만 카운트
        //    (BookingSeat 테이블에는 실제로 예매 시도가 있었던 좌석만 존재하므로,
        //     이 목록 자체를 구역의 전체 좌석으로 착각하면 안 됨!)
        Map<String, Long> selectedCountBySection = new HashMap<>();
        for (BookingSeat seat : allSeats) {
            if (seat.getStatus() == BookingStatus.SELECTED) {
                selectedCountBySection.merge(seat.getSectionName(), 1L, Long::sum);
            }
        }

        // 매진된 구역명들을 담을 리스트 생성
        List<String> soldOutSections = new ArrayList<>();

        // 2. 각 구역의 "예매 확정 좌석 수"를 해당 구역의 "실제 총 좌석 수"와 비교해서 매진 여부 판단
        for (Map.Entry<String, Long> entry : selectedCountBySection.entrySet()) {
            String sectionName = entry.getKey();
            long selectedCount = entry.getValue();

            long totalSeatsInSection = seatRepository.countByPerformancePerformanceIdAndSectionName(performanceId, sectionName);

            if (totalSeatsInSection > 0 && selectedCount >= totalSeatsInSection) {
                soldOutSections.add(sectionName);
            }
        }

        // 3. 매진된 구역이 하나 이상 존재할 경우 동적 문자열 조립
        if (!soldOutSections.isEmpty()) {
            Collections.sort(soldOutSections);

            String combinedSections = String.join(", ", soldOutSections);

            result.put("status", "SOLD_OUT");
            result.put("section", combinedSections);
            result.put("notice", combinedSections + "의 모든 좌석이 매진되었습니다. 다른 구역을 선택해 주세요!");
            return result;
        }

        result.put("status", "AVAILABLE");
        return result;
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
            gradeSet.add(rs.getSeatGrade());
            seatPriceSum += rs.getPrice();
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
                if (rs.getSeatGrade().equals(grade)) {
                    count++;
                    priceSum += rs.getPrice();
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

    public List<String> getSeatGrades() {
        return List.of("VIP", "R", "S");
    }
}
package com.clearticket.clearticket.service;


import com.clearticket.clearticket.model.dto.*;
import com.clearticket.clearticket.model.entity.*;
import com.clearticket.clearticket.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MyPageService {
    private final ReservationRepository reservationRepository;
    private final WaitingRepository waitingRepository;
    private final SeatRepository seatRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    private final BookingSeatsRepository bookingSeatsRepository;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    /**
     * 사용자 통계 및 취향분석 조회
     * @param userId 로그인한 사용자의 고유 ID
     * @return 총 관람시간, 총 관람편수, 장르비율 리스트, 다음 공연 디데이가 포함된 통계 DTO
     */
    @Transactional(readOnly = true)
    public MyPageStatisticsResponseDto getUserStatistics(Long userId) {
        List<Reservation> reservations = reservationRepository.findByUserUserIdAndStatus(userId, ReservationStatus.CONFIRMED);

        int totalWatchingTime = 0;
        int totalCount = reservations.size();

        Map<String, Integer> genreCounts = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime closestFutureShow = null;

        for (Reservation reservation : reservations) {
            Schedule schedule = reservation.getSchedule();
            if (schedule == null) {
                continue;
            }

            Performance performance = schedule.getPerformance();
            if (performance == null) {
                continue;
            }

            // 총 관람 시간 누적
            totalWatchingTime += performance.getRuntime();

            // 장르 카운트 누적
            String genre = performance.getGenre();
            if (genre != null) {
                genreCounts.put(genre, genreCounts.getOrDefault(genre, 0) + 1);
            }

            // 가장 가까운 미래의 공연 시간 찾기
            if (schedule.getShowDate() != null && schedule.getShowTime() != null) {
                LocalDateTime showDateTime = LocalDateTime.of(schedule.getShowDate(), schedule.getShowTime());
                if (showDateTime.isAfter(now)) {
                    if (closestFutureShow == null || showDateTime.isBefore(closestFutureShow)) {
                        closestFutureShow = showDateTime;
                    }
                }
            }
        }

        // 장르 비율 계산 및 DTO 리스트 추가
        List<GenreRatioDto> genreRatios = new ArrayList<>();
        if (totalCount > 0) {
            for (Map.Entry<String, Integer> entry : genreCounts.entrySet()) {
                double percentage = Math.round(((double) entry.getValue() / totalCount) * 1000) / 10.0;
                genreRatios.add(new GenreRatioDto(entry.getKey(), percentage));
            }
            // 퍼센트 높은 순 정렬
            genreRatios.sort((a, b) -> Double.compare(b.getPercentage(), a.getPercentage()));
        }

        // 디데이 문자열 계산
        String nextPerformanceDDay = "관람 예정 공연 없음";
        if (closestFutureShow != null) {
            long daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), closestFutureShow.toLocalDate());
            if (daysBetween == 0) {
                nextPerformanceDDay = "두근두근, 바로 오늘 클리어! D-Day";
            } else {
                nextPerformanceDDay = "두근두근, 다음 클리어까지 D-" + daysBetween;
            }
        }

        return new MyPageStatisticsResponseDto(totalWatchingTime, totalCount, genreRatios, nextPerformanceDDay);
    }


    /**
     * 내 예매내역 전체 리스트 조회
     * @param userId 로그인한 사용자의 고유 ID
     * @return 마이페이지용 예매내역 응답 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<MyPageReservationResponseDto> getMyReservations(Long userId) {
        List<Reservation> reservations = reservationRepository.findByUserUserId(userId);
        List<MyPageReservationResponseDto> result = new ArrayList<>();

        for (Reservation reservation : reservations) {
            result.add(toMyPageResponseDto(reservation));
        }

        return result;
    }


    /**
     * 내 예매내역 상세 정보 DTO 변환
     * @param reservation 데이터베이스에서 꺼내온 사용자의 예매내역 원본 데이터
     * @return 마이페이지용 예매내역 응답 DTO
     */
    private MyPageReservationResponseDto toMyPageResponseDto(Reservation reservation) {
        Schedule schedule = reservation.getSchedule();
        Performance performance = (schedule != null) ? schedule.getPerformance() : null;

        LocalDateTime showDateTime = null;
        if (schedule != null && schedule.getShowDate() != null && schedule.getShowTime() != null) {
            showDateTime = LocalDateTime.of(schedule.getShowDate(), schedule.getShowTime());
        }

        return new MyPageReservationResponseDto(
                reservation.getReservationId(),
                reservation.getReservationNumber(),
                reservation.getCreatedAt(),
                performance != null ? performance.getTitle() : "공연 정보 없음",
                performance != null ? performance.getPosterUrl() : null,
                showDateTime,
                performance != null && performance.getVenue() != null ? performance.getVenue().getName() : "장소 정보 없음",
                reservation.getStatus() != null ? reservation.getStatus().name() : null
        );
    }


    /**
     * 예매대기 상세 정보 DTO 변환
     * @param waiting 데이터베이스에서 꺼내온 사용자의 예매대기 원본 데이터
     * @return 마이페이지용 예매대기 응답 DTO
     */
    private MyPageWaitingResponseDto toMyPageWaitingResponseDto(Waiting waiting) {
        Schedule schedule = waiting.getSchedule();
        Performance performance = (schedule != null) ? schedule.getPerformance() : null;

        LocalDateTime showDateTime = null;
        if (schedule != null && schedule.getShowDate() != null && schedule.getShowTime() != null) {
            showDateTime = LocalDateTime.of(schedule.getShowDate(), schedule.getShowTime());
        }


        int waitingOrder = 1; // 기본값 1
        if (schedule != null) {
            int aheadCount = waitingRepository.countByScheduleAndCreatedAtBeforeAndStatus(
                    schedule,
                    waiting.getCreatedAt(),
                    WaitingStatus.WAITING
            );
            waitingOrder = aheadCount + 1;
        }

        Seat seat = waiting.getSeat();
        String seatInfo = "좌석 정보 없음";

        if (seat != null) {
            seatInfo = seat.getSeatGrade() + " " +
                    seat.getSectionName() + " " +
                    seat.getRowNum() + "열 " +
                    seat.getSeatNum() + "번";
        }

        return new MyPageWaitingResponseDto(
                waiting.getWaitingId(),
                performance != null ? performance.getTitle() : "공연 정보 없음",
                performance != null ? performance.getPosterUrl() : null,
                performance != null && performance.getVenue() != null ? performance.getVenue().getName() : "장소 정보 없음",
                showDateTime,
                waiting.getCreatedAt(),
                seatInfo,
                waitingOrder,
                waiting.getStatus() != null ? waiting.getStatus().name() : null
        );
    }


    /**
     * 내 예매대기 목록 조회
     * @param userId 로그인한 사용자의 고유 ID
     * @return 마이페이지용 예매대기 응답 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<MyPageWaitingResponseDto> getMyWaitings(Long userId) {
        List<Waiting> waitings = waitingRepository.findByUserUserId(userId);
        List<MyPageWaitingResponseDto> result = new ArrayList<>();

        for (Waiting waiting : waitings) {
            result.add(toMyPageWaitingResponseDto(waiting));
        }

        return result;
    }


    /**
     * 마이페이지 예매 상세 정보 조회 (본인 소유 검증 포함)
     * @param reservationId 조회할 예매 ID
     * @param userId 로그인한 사용자의 고유 ID (권한 검증용)
     */
    @Transactional(readOnly = true)
    public MyPageReservationDetailResponseDto getReservationDetail(Long reservationId, Long userId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매 내역입니다. ID: " + reservationId));

        if (!reservation.getUser().getUserId().equals(userId)) {
            throw new IllegalStateException("본인의 예매 내역만 조회할 수 있습니다.");
        }

        return buildDetailDto(reservation);
    }

    /**
     * 예매 취소 실행 (본인 소유 검증 + 취소 수수료 계산 + 선점 좌석 반환)
     * @param reservationId 취소할 예매 ID
     * @param userId 로그인한 사용자의 고유 ID (권한 검증용)
     * @return 취소 처리 이후의 최종 상태 DTO (환불 예정 금액 포함)
     */
    @Transactional
    public MyPageReservationDetailResponseDto cancelReservation(Long reservationId, Long userId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매 내역입니다. ID: " + reservationId));

        if (!reservation.getUser().getUserId().equals(userId)) {
            throw new IllegalStateException("본인의 예매 내역만 취소할 수 있습니다.");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELED) {
            throw new IllegalStateException("이미 취소된 예매 내역입니다.");
        }

        Schedule schedule = reservation.getSchedule();
        if (schedule != null && schedule.getShowDate() != null && schedule.getShowTime() != null) {
            LocalDateTime showDateTime = LocalDateTime.of(schedule.getShowDate(), schedule.getShowTime());
            if (!showDateTime.isAfter(LocalDateTime.now())) {
                throw new IllegalStateException("이미 관람일이 지난 예매는 취소할 수 없습니다.");
            }
        }

        // 1. 예매 상태를 취소로 변경
        reservation.changeStatus(ReservationStatus.CANCELED);

        // 2. 선점(예매 확정)되어 있던 실물 좌석을 다시 예매 가능한 상태로 반환
        if (schedule != null) {
            for (ReservationSeat rs : reservation.getReservationSeats()) {
                bookingSeatsRepository.searchByScheduleAndSeat(
                        schedule.getScheduleId(),
                        rs.getSectionName(),
                        rs.getRowNum(),
                        rs.getSeatNum()
                ).ifPresent(bookingSeatsRepository::delete);

                messagingTemplate.convertAndSend(
                        "/topic/seats/" + schedule.getScheduleId(),
                        new com.clearticket.clearticket.model.dto.seat.SeatStatusEvent(
                                rs.getSectionName(), rs.getRowNum(), rs.getSeatNum(), "AVAILABLE")
                );
            }
        }

        return buildDetailDto(reservation);
    }

    /**
     * Reservation 엔티티 -> 마이페이지 예매 상세 DTO 변환 (취소 수수료 계산 포함)
     */
    private MyPageReservationDetailResponseDto buildDetailDto(Reservation reservation) {
        Schedule schedule = reservation.getSchedule();
        Performance performance = (schedule != null) ? schedule.getPerformance() : null;

        LocalDateTime showDateTime = null;
        if (schedule != null && schedule.getShowDate() != null && schedule.getShowTime() != null) {
            showDateTime = LocalDateTime.of(schedule.getShowDate(), schedule.getShowTime());
        }

        List<ReservationSeat> reservationSeats = reservation.getReservationSeats();
        List<MyPageReservationSeatDto> seatDtos = new ArrayList<>();
        int ticketPriceSum = 0;
        for (ReservationSeat rs : reservationSeats) {
            seatDtos.add(new MyPageReservationSeatDto(
                    rs.getResSeatId(), rs.getSectionName(), rs.getRowNum(),
                    rs.getSeatNum(), rs.getSeatGrade(), rs.getPrice()));
            ticketPriceSum += (rs.getPrice() != null ? rs.getPrice() : 0);
        }

        int discountAmount = 0;
        if (reservation.getUsedCoupon() != null) {
            String type = String.valueOf(reservation.getUsedCoupon().getDiscountType());
            int val = reservation.getUsedCoupon().getDiscountValue();
            if ("PERCENT".equals(type)) {
                discountAmount = (int) Math.floor(ticketPriceSum * (val / 100.0));
            } else if ("AMOUNT".equals(type)) {
                discountAmount = val;
            }
        }

        boolean isCancelableStatus = reservation.getStatus() == ReservationStatus.CONFIRMED
                || reservation.getStatus() == ReservationStatus.WAITING;

        long daysBeforeShow = Long.MAX_VALUE;
        if (showDateTime != null) {
            daysBeforeShow = ChronoUnit.DAYS.between(LocalDate.now(), showDateTime.toLocalDate());
        }

        boolean showAlreadyPassed = showDateTime != null && !showDateTime.isAfter(LocalDateTime.now());
        boolean canCancel = isCancelableStatus && !showAlreadyPassed;

        int feeRatePercent;
        String deadlineText;
        if (daysBeforeShow >= 10) {
            feeRatePercent = 0;
            deadlineText = "관람일 10일 전까지 수수료 없이 취소 가능합니다.";
        } else if (daysBeforeShow >= 7) {
            feeRatePercent = 10;
            deadlineText = "관람일 9일 전 ~ 7일 전: 장당 4,000원(티켓금액의 10% 한도)";
        } else if (daysBeforeShow >= 4) {
            feeRatePercent = 10;
            deadlineText = "관람일 6일 전 ~ 4일 전: 티켓금액의 10%";
        } else if (daysBeforeShow >= 1) {
            feeRatePercent = 30;
            deadlineText = "관람일 3일 전 ~ 1일 전: 티켓금액의 30%";
        } else {
            feeRatePercent = 100;
            deadlineText = "관람일 당일 이후에는 취소가 불가능합니다.";
            canCancel = false;
        }

        int cancelFeeAmount = (int) Math.floor(ticketPriceSum * (feeRatePercent / 100.0));
        int totalPrice = reservation.getTotalPrice();
        int refundAmount = Math.max(0, totalPrice - cancelFeeAmount);

        if (reservation.getStatus() == ReservationStatus.CANCELED) {
            canCancel = false;
        }

        return new MyPageReservationDetailResponseDto(
                reservation.getReservationId(),
                reservation.getReservationNumber(),
                reservation.getCreatedAt(),
                performance != null ? performance.getTitle() : "공연 정보 없음",
                performance != null ? performance.getPosterUrl() : null,
                performance != null && performance.getVenue() != null ? performance.getVenue().getName() : "장소 정보 없음",
                showDateTime,
                reservation.getStatus() != null ? reservation.getStatus().name() : null,
                reservation.getTicketType(),
                reservation.getRecipientName(),
                reservation.getRecipientPhone(),
                reservation.getShippingAddress(),
                seatDtos,
                ticketPriceSum,
                reservation.getShippingFee(),
                discountAmount,
                totalPrice,
                canCancel,
                deadlineText,
                feeRatePercent,
                cancelFeeAmount,
                refundAmount
        );
    }


    /**
     * 예매대기 취소 (상태 변경 방식)
     * @param waitingId 취소하고자 하는 예매대기 고유 ID
     * @param userId 로그인한 사용자의 고유 ID (권한 검증용)
     */
    @Transactional
    public void cancelWaitingById(Long waitingId, Long userId) {
        Optional<Waiting> optionalWaiting = waitingRepository.findById(waitingId);

        if (optionalWaiting.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 예매대기 내역입니다. ID: " + waitingId);
        }

        Waiting waiting = optionalWaiting.get();

        if (!waiting.getUser().getUserId().equals(userId)) {
            throw new IllegalStateException("본인의 예매대기 내역만 취소할 수 있습니다.");
        }

        if (waiting.getStatus() != WaitingStatus.WAITING) {
            throw new IllegalStateException("이미 취소되었거나 처리가 완료된 대기 내역입니다.");
        }

        waiting.changeStatus(WaitingStatus.EXPIRED);
    }



    // ======================================================
    // 마이페이지 회원정보 및 배송지관리 관련 서비스
    // ======================================================

    /**
     * 마이페이지 프로필 첫 화면에 보이는 회원의 상세 정보
     * @param userId 로그인한 사용자
     * @return 프로필 정보
     */
    @Transactional(readOnly = true)
    public MyPageProfileResponseDto getUserProfile(Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 회원입니다. ID: " + userId);
        }

        User user = userOptional.get();
        return new MyPageProfileResponseDto(
                user.getUserId(),
                user.getEmail(),
                user.getName(),
                user.getEmail(),
                user.getPhone()
        );
    }

    /**
     * 배송지 수정
     * @param addressId 수정할 배송지 ID
     * @param userId 로그인한 회원 (권한 검증용)
     * @param requestDto 수정 요청 데이터
     */
    @Transactional
    public void updateAddress(Long addressId, Long userId, MyPageAddressSaveRequestDto requestDto) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배송지 내역입니다. ID: " + addressId));

        if (!address.getUser().getUserId().equals(userId)) {
            throw new IllegalStateException("본인의 배송지 정보만 수정할 수 있습니다.");
        }

        // 기본 배송지로 변경하는 경우, 기존 기본 배송지 해제
        if (requestDto.isDefault() && !address.isDefault()) {
            addressRepository.findByUserAndIsDefaultTrue(address.getUser())
                    .ifPresent(old -> old.changeDefaultStatus(false));
        }

        address.updateAddress(
                requestDto.getAddressName(),
                requestDto.getRecipientName(),
                requestDto.getRecipientPhone(),
                requestDto.getZonecode(),
                requestDto.getRoadAddress(),
                requestDto.getDetailAddress(),
                requestDto.isDefault()
        );
    }
    /**
     * 프로필 수정 새 정보 업데이트
     * @param userId 로그인한 회원
     * @param requestDto 화면에서 수정요청한 데이터
     */
    private static final String PHONE_REGEX = "^01[0-9]-\\d{3,4}-\\d{4}$";

    @Transactional
    public void updateUserProfile(Long userId, MyPageProfileUpdateRequestDto requestDto) {
        Optional<User> optionalUser = userRepository.findById(userId);

        if (optionalUser.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 회원입니다. ID: " + userId);
        }

        User user = optionalUser.get();

        // 이메일(아이디)과 이름은 가입 시 확정되는 값으로 이 화면에서 변경할 수 없다.
        // 요청에 값이 와도 무시하고, 전화번호만 정규식 검증 후 갱신한다.
        String phone = requestDto.getPhone();
        if (phone != null && !phone.isBlank()) {
            if (!phone.matches(PHONE_REGEX)) {
                throw new IllegalArgumentException("올바른 전화번호 형식이 아닙니다. (예: 010-1234-5678)");
            }
            user.setPhone(phone);
        }
    }

    /**
     * 마이페이지 배송지 목록 전체 조회
     * @param userId 로그인한 회원
     * @return 배송지 정보 리스트
     */
    @Transactional(readOnly = true)
    public List<MyPageAddressResponseDto> getAddressList(Long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);

        if (optionalUser.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 회원입니다. ID: " + userId);
        }

        User user = optionalUser.get();

        List<Address> addresses = addressRepository.findByUser(user);
        List<MyPageAddressResponseDto> result = new ArrayList<>();

        for (Address addr : addresses) {
            result.add(new MyPageAddressResponseDto(
                    addr.getAddressId(),
                    addr.getAddressName(),
                    addr.getRecipientName(),
                    addr.getRecipientPhone(),
                    addr.getZonecode(),
                    addr.getRoadAddress(),
                    addr.getDetailAddress(),
                    addr.isDefault()
            ));
        }
        return result;
    }


    /**
     * 신규 배송지 등록
     * @param userId 로그인한 회원
     * @param requestDto 신규 배송지 등록 요청 데이터
     */
    @Transactional
    public void addAddress(Long userId, MyPageAddressSaveRequestDto requestDto) {
        Optional<User> optionalUser = userRepository.findById(userId);

        if (optionalUser.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 회원입니다. ID: " + userId);
        }

        User user = optionalUser.get();

        // 신규 배송지가 기본 배송지로 설정된 경우, 기존 기본 배송지 상태를 비활성화(false) 처리
        if (requestDto.isDefault()) {
            Optional<Address> oldDefaultAddress = addressRepository.findByUserAndIsDefaultTrue(user);
            if (oldDefaultAddress.isPresent()) {
                Address oldDefault = oldDefaultAddress.get();
                oldDefault.changeDefaultStatus(false); // 기존 기본 주소를 일반 주소로 변경
            }
        }

        Address newAddress = new Address(
                null,
                user,
                requestDto.getAddressName(),
                requestDto.getRecipientName(),
                requestDto.getRecipientPhone(),
                requestDto.getZonecode(),
                requestDto.getRoadAddress(),
                requestDto.getDetailAddress(),
                requestDto.isDefault()
        );

        addressRepository.save(newAddress);
    }


    /**
     * 배송지 삭제
     * @param addressId 삭제하려는 배송지 ID
     * @param userId 로그인한 회원
     */
    @Transactional
    public void deleteAddress(Long addressId, Long userId) {
        Optional<Address> optionalAddress = addressRepository.findById(addressId);

        if (optionalAddress.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 배송지 내역입니다. ID: " + addressId);
        }

        Address address = optionalAddress.get();

        if (!address.getUser().getUserId().equals(userId)) {
            throw new IllegalStateException("본인의 배송지 정보만 삭제할 수 있습니다.");
        }

        addressRepository.delete(address);
    }

    @Transactional(readOnly = true)
    public List<MyPageCouponResponseDto> getCouponList(Long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);

        if (optionalUser.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 회원입니다. ID: " + userId);
        }

        User user = optionalUser.get();

        List<Coupon> coupons = couponRepository.findByUser(user);
        List<MyPageCouponResponseDto> result = new ArrayList<>();

        for (Coupon coupon : coupons) {
            String discountTypeStr = coupon.getDiscountType() != null ? String.valueOf(coupon.getDiscountType()) : null;
            String statusStr = coupon.getCouponStatus() != null ? String.valueOf(coupon.getCouponStatus()) : null;

            result.add(new MyPageCouponResponseDto(
                    coupon.getCouponId(),
                    coupon.getCouponName(),
                    coupon.getDiscountValue(),
                    discountTypeStr,
                    coupon.getExpiryDate() != null ? coupon.getExpiryDate().atStartOfDay() : null,
                    statusStr
            ));
        }

        return result;
    }


}
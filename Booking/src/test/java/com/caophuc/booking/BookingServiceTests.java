package com.caophuc.booking;

import com.caophuc.booking.client.CourtClient;
import com.caophuc.booking.client.CourtDto;
import com.caophuc.booking.client.UserClient;
import com.caophuc.booking.client.UserDto;
import com.caophuc.booking.dto.BookingRequest;
import com.caophuc.booking.dto.BookingResponse;
import com.caophuc.booking.model.Booking;
import com.caophuc.booking.model.TimeSlot;
import com.caophuc.booking.repository.BookingRepository;
import com.caophuc.booking.repository.TimeSlotRepository;
import com.caophuc.booking.service.impl.BookingServiceImpl;
import com.caophuc.booking.service.kafka.BookingProducerService;
import com.caophuc.booking.util.BookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// BƯỚC 2: KÍCH HOẠT MOCKITO
@ExtendWith(MockitoExtension.class)
class BookingServiceTests {

    // BƯỚC 3: TẠO CÁC ĐỐI TƯỢNG GIẢ (MOCKS)
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TimeSlotRepository timeSlotRepository;
    @Mock
    private CourtClient courtClient;
    @Mock
    private UserClient userClient;
    @Mock
    private BookingProducerService bookingProducerService;

    // BƯỚC 4: TIÊM CÁC MOCKS VÀO SERVICE CẦN TEST
    @InjectMocks
    private BookingServiceImpl bookingService;

    private TimeSlot availableSlot;
    private Booking booking;
    private UserDto userDto;
    private CourtDto courtDto;

    // BƯỚC 5: THIẾT LẬP DỮ LIỆU CHUNG CHO CÁC TEST
    @BeforeEach
    void setUp() {
        // Dữ liệu mẫu dùng chung cho nhiều test case
        availableSlot = TimeSlot.builder()
                .id(1)
                .courtId(1)
                .startTime(LocalDateTime.now().plusHours(1))
                .endTime(LocalDateTime.now().plusHours(2))
                .isAvailable(true)
                .price(100.0)
                .build();

        booking = Booking.builder()
                .id(1)
                .userId(1)
                .timeSlots(List.of(availableSlot))
                .status(BookingStatus.CONFIRMED)
                .totalPrice(100.0)
                .createdAt(LocalDateTime.now())
                .build();

        userDto = new UserDto(); // Giả sử UserDto có các setter hoặc constructor phù hợp
        userDto.setEmail("test@example.com");
        userDto.setFcmToken("some-fcm-token");

        courtDto = new CourtDto(); // Giả sử CourtDto có các setter
        courtDto.setName("Sân Tennis Quận 1");
        courtDto.setAddress("123 Nguyễn Du, P. Bến Thành, Q.1");
    }

    // BƯỚC 6: VIẾT TEST CASE ĐẦU TIÊN (HAPPY PATH)
    @Test
    void createBooking_Success_WhenSlotIsAvailable() {
        // 1. ARRANGE (SẮP ĐẶT)
        BookingRequest request = new BookingRequest();
        request.setTimeSlotIds(List.of(1));
        request.setNote("Test note");

        // Giả lập: Khi timeSlotRepository.findById(1) được gọi, trả về slot có sẵn
        when(timeSlotRepository.findById(1)).thenReturn(Optional.of(availableSlot));

        // Giả lập: Khi bookingRepository.save() được gọi với bất kỳ object Booking nào,
        // trả về chính object đó và gán ID cho nó.
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking savedBooking = invocation.getArgument(0);
            savedBooking.setId(1); // Giả lập việc DB đã gán ID
            return savedBooking;
        });

        // Giả lập: Khi gọi User và Court client, trả về thông tin mẫu
        when(userClient.getUserById(1)).thenReturn(userDto);
        when(courtClient.getCourtById(1)).thenReturn(courtDto);


        // 2. ACT (THỰC THI)
        BookingResponse response = bookingService.createBooking(request, 1);


        // 3. ASSERT (KIỂM CHỨNG)
        assertNotNull(response);
        assertEquals(BookingStatus.PENDING, response.getStatus());
        assertEquals(100.0, response.getTotalPrice());
        assertFalse(availableSlot.isAvailable()); // Quan trọng: Kiểm tra slot đã bị khóa

        // Kiểm tra xem các phương thức quan trọng có được gọi không
        verify(bookingRepository, times(1)).save(any(Booking.class));
        verify(bookingProducerService, times(1)).sendNotification(any());
    }

    @Test
    void createBooking_Fail_WhenSlotIsNotAvailable() {
        // 1. ARRANGE
        availableSlot.setAvailable(false); // Set slot này đã bị đặt
        BookingRequest request = new BookingRequest();
        request.setTimeSlotIds(List.of(1));

        when(timeSlotRepository.findById(1)).thenReturn(Optional.of(availableSlot));

        // 2. ACT & 3. ASSERT
        // Kiểm tra xem có đúng Exception được ném ra không
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            bookingService.createBooking(request, 1);
        });

        assertTrue(exception.getMessage().contains("đã bị người khác đặt"));

        // Đảm bảo rằng không có booking nào được lưu nếu có lỗi
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void cancelBooking_Success_WhenUserIsOwner() {
        // 1. ARRANGE
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));

        // 2. ACT
        bookingService.cancelBooking(1, 1); // userId = 1 là chủ của booking

        // 3. ASSERT
        // Sử dụng ArgumentCaptor để "bắt" lại đối tượng được truyền vào hàm save()
        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        Booking savedBooking = bookingCaptor.getValue();

        assertEquals(BookingStatus.CANCELLED, savedBooking.getStatus());
        assertTrue(availableSlot.isAvailable()); // Kiểm tra slot đã được nhả ra

        verify(timeSlotRepository, times(1)).save(availableSlot);
        verify(bookingProducerService, times(1)).sendNotification(any());
    }

    @Test
    void cancelBooking_Fail_WhenUserIsNotOwner() {
        // 1. ARRANGE
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        Integer wrongUserId = 2; // User này không phải chủ booking

        // 2. ACT & 3. ASSERT
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.cancelBooking(1, wrongUserId);
        });

        assertEquals("Bạn không có quyền hủy đơn đặt sân này.", exception.getMessage());
        verify(bookingRepository, never()).save(any()); // Đảm bảo không có gì được lưu
    }

    @Test
    void getBookingById_Success_WhenBookingExists() {
        // 1. ARRANGE
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(courtClient.getCourtById(1)).thenReturn(courtDto);

        // 2. ACT
        Optional<BookingResponse> response = bookingService.getBookingById(1);

        // 3. ASSERT
        assertTrue(response.isPresent());
        assertEquals(1, response.get().getId());
        assertEquals("Sân Tennis Quận 1", response.get().getCourtName());
    }

    @Test
    void getBookingById_Fail_WhenBookingDoesNotExist() {
        // 1. ARRANGE
        when(bookingRepository.findById(99)).thenReturn(Optional.empty());

        // 2. ACT
        Optional<BookingResponse> response = bookingService.getBookingById(99);

        // 3. ASSERT
        assertTrue(response.isEmpty());
    }
}
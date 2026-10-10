package com.example.roombooking.service.impl;

import com.example.roombooking.domain.entity.Booking;
import com.example.roombooking.domain.entity.BookingEquipment;
import com.example.roombooking.domain.entity.Equipment;
import com.example.roombooking.domain.entity.MeetingRoom;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.BookingStatus;
import com.example.roombooking.domain.enums.RoomStatus;
import com.example.roombooking.domain.enums.RoomType;
import com.example.roombooking.dto.request.BookingCreateRequest;
import com.example.roombooking.dto.response.BookingResponse;
import com.example.roombooking.event.BookingStatusChangedEvent;
import com.example.roombooking.exception.InvalidStateTransitionException;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.exception.RoomNotAvailableException;
import com.example.roombooking.mapper.BookingMapper;
import com.example.roombooking.repository.BookingRepository;
import com.example.roombooking.repository.EquipmentRepository;
import com.example.roombooking.repository.UserRepository;
import com.example.roombooking.service.strategy.BookingRuleStrategyFactory;
import com.example.roombooking.service.strategy.StandardRoomRuleStrategy;
import com.example.roombooking.service.strategy.VipRoomRuleStrategy;
import com.example.roombooking.service.validation.BookingValidationChain;
import com.example.roombooking.service.validation.BookingValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for BookingServiceImpl (Booking Core).
 * The validation chain is mocked; the mapper and strategy factory are real objects.
 * A VIP room is used so new bookings stay PENDING (auto-approval of STANDARD rooms
 * is covered by BookingServiceImplStrategyTest).
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    private static final LocalDateTime START = LocalDateTime.of(2030, 1, 1, 9, 0);
    private static final LocalDateTime END = START.plusHours(2);

    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;
    @Mock private EquipmentRepository equipmentRepository;
    @Mock private BookingValidationChain validationChain;
    @Mock private ApplicationEventPublisher eventPublisher;

    private BookingServiceImpl bookingService;
    private User requester;
    private User otherUser;
    private MeetingRoom vipRoom;

    @BeforeEach
    void setUp() {
        BookingRuleStrategyFactory factory = new BookingRuleStrategyFactory(
                List.of(new StandardRoomRuleStrategy(), new VipRoomRuleStrategy()));
        bookingService = new BookingServiceImpl(bookingRepository, userRepository, equipmentRepository,
                validationChain, new BookingMapper(), eventPublisher, factory);

        requester = user(1L, "sol");
        otherUser = user(5L, "friend");

        vipRoom = new MeetingRoom();
        vipRoom.setId(2L);
        vipRoom.setName("VIP Room");
        vipRoom.setCapacity(10);
        vipRoom.setRoomType(RoomType.VIP);
        vipRoom.setStatus(RoomStatus.AVAILABLE);
    }

    // ---------- helpers ----------

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private static Equipment equipment(Long id, String name) {
        Equipment equipment = new Equipment();
        equipment.setId(id);
        equipment.setName(name);
        equipment.setTotalQuantity(5);
        return equipment;
    }

    private BookingCreateRequest request() {
        BookingCreateRequest request = new BookingCreateRequest();
        request.setRoomId(2L);
        request.setStartTime(START);
        request.setEndTime(END);
        request.setPurpose("Client demo");
        return request;
    }

    private Booking bookingWithStatus(BookingStatus status) {
        return Booking.builder()
                .id(100L)
                .user(requester)
                .room(vipRoom)
                .startTime(START)
                .endTime(END)
                .status(status)
                .purpose("Sprint planning")
                .build();
    }

    // The chain is mocked, so simulate what the real handlers put into the context:
    // RoomAvailabilityHandler sets the room, EquipmentAvailabilityHandler fills merged quantities.
    private void chainPasses(Map<Long, Integer> equipmentQuantities) {
        doAnswer(invocation -> {
            BookingValidationContext context = invocation.getArgument(0);
            context.setRoom(vipRoom);
            context.getRequestedEquipmentQuantities().putAll(equipmentQuantities);
            return null;
        }).when(validationChain).validate(any(BookingValidationContext.class));
    }

    private void saveAssignsIdAndReturnsBooking() {
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            if (booking.getId() == null) {
                booking.setId(100L);
            }
            return booking;
        });
    }

    // ---------- createBooking ----------

    @Test
    void createBooking_vipRoom_savesPendingBookingForRequester() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        chainPasses(Map.of());
        saveAssignsIdAndReturnsBooking();

        BookingResponse response = bookingService.createBooking(request(), 1L);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getRoomName()).isEqualTo("VIP Room");
        assertThat(response.getEquipmentItems()).isEmpty();
    }

    @Test
    void createBooking_forAnotherUser_savesUnderThatUser() {
        BookingCreateRequest request = request();
        request.setBookingForUserId(5L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(userRepository.findById(5L)).thenReturn(Optional.of(otherUser));
        chainPasses(Map.of());
        saveAssignsIdAndReturnsBooking();

        BookingResponse response = bookingService.createBooking(request, 1L);

        assertThat(response.getUserId()).isEqualTo(5L);
        assertThat(response.getUsername()).isEqualTo("friend");
    }

    @Test
    void createBooking_attachesEquipmentApprovedByChain() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        chainPasses(Map.of(10L, 3));
        when(equipmentRepository.getReferenceById(10L)).thenReturn(equipment(10L, "Projector"));
        saveAssignsIdAndReturnsBooking();

        BookingResponse response = bookingService.createBooking(request(), 1L);

        assertThat(response.getEquipmentItems()).hasSize(1);
        assertThat(response.getEquipmentItems().get(0).getEquipmentName()).isEqualTo("Projector");
        assertThat(response.getEquipmentItems().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    void createBooking_ignoresBookingIdSentByClient() {
        BookingCreateRequest request = request();
        request.setBookingId(55L); // must not be usable to skip the overlap check
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        chainPasses(Map.of());
        saveAssignsIdAndReturnsBooking();

        bookingService.createBooking(request, 1L);

        ArgumentCaptor<BookingValidationContext> captor = ArgumentCaptor.forClass(BookingValidationContext.class);
        verify(validationChain).validate(captor.capture());
        assertThat(captor.getValue().getRequest().getBookingId()).isNull();
        assertThat(captor.getValue().getRequester()).isSameAs(requester);
    }

    @Test
    void createBooking_withoutRequesterId_throwsIllegalArgument() {
        assertThatThrownBy(() -> bookingService.createBooking(request(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("X-User-Id");

        verifyNoInteractions(userRepository, validationChain, bookingRepository);
    }

    @Test
    void createBooking_unknownRequester_throwsNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(request(), 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verifyNoInteractions(validationChain, bookingRepository);
    }

    @Test
    void createBooking_whenChainRejects_doesNotSave() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        doThrow(new RoomNotAvailableException("Room is already booked"))
                .when(validationChain).validate(any(BookingValidationContext.class));

        assertThatThrownBy(() -> bookingService.createBooking(request(), 1L))
                .isInstanceOf(RoomNotAvailableException.class);

        verify(bookingRepository, never()).save(any());
    }

    // ---------- read ----------

    @Test
    void getBookingById_returnsMappedResponse() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(bookingWithStatus(BookingStatus.PENDING)));

        BookingResponse response = bookingService.getBookingById(100L);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getUsername()).isEqualTo("sol");
        assertThat(response.getPurpose()).isEqualTo("Sprint planning");
    }

    @Test
    void getBookingById_unknownId_throwsNotFound() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBookingById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Booking not found");
    }

    @Test
    void getBookingsByRoom_passesPageableAndMapsEveryBooking() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("startTime"));
        when(bookingRepository.findByRoomId(2L, pageable))
                .thenReturn(new PageImpl<>(List.of(bookingWithStatus(BookingStatus.PENDING)), pageable, 1));

        Page<BookingResponse> page = bookingService.getBookingsByRoom(2L, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(BookingResponse::getId).containsExactly(100L);
    }

    @Test
    void getBookingsByUser_passesPageableAndMapsEveryBooking() {
        Pageable pageable = PageRequest.of(1, 5);
        when(bookingRepository.findByUserId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(bookingWithStatus(BookingStatus.APPROVED)), pageable, 6));

        Page<BookingResponse> page = bookingService.getBookingsByUser(1L, pageable);

        assertThat(page.getTotalElements()).isEqualTo(6);
        assertThat(page.getContent().get(0).getStatus()).isEqualTo(BookingStatus.APPROVED);
    }

    // ---------- updateBooking ----------

    @Test
    void updateBooking_pendingBooking_updatesDetailsAndPassesOwnIdToChain() {
        BookingCreateRequest request = request();
        request.setPurpose("Moved to next week");
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(bookingWithStatus(BookingStatus.PENDING)));
        chainPasses(Map.of());
        saveAssignsIdAndReturnsBooking();

        BookingResponse response = bookingService.updateBooking(100L, request, 1L);

        assertThat(response.getPurpose()).isEqualTo("Moved to next week");
        assertThat(response.getStatus()).isEqualTo(BookingStatus.PENDING);

        ArgumentCaptor<BookingValidationContext> captor = ArgumentCaptor.forClass(BookingValidationContext.class);
        verify(validationChain).validate(captor.capture());
        assertThat(captor.getValue().getRequest().getBookingId()).isEqualTo(100L);
    }

    @Test
    void updateBooking_replacesOldEquipmentWithNewSelection() {
        Booking booking = bookingWithStatus(BookingStatus.PENDING);
        booking.getBookingEquipments().add(new BookingEquipment(null, booking, equipment(9L, "Old mic"), 1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        chainPasses(Map.of(10L, 2));
        when(equipmentRepository.getReferenceById(10L)).thenReturn(equipment(10L, "Projector"));
        saveAssignsIdAndReturnsBooking();

        BookingResponse response = bookingService.updateBooking(100L, request(), 1L);

        assertThat(response.getEquipmentItems()).hasSize(1);
        assertThat(response.getEquipmentItems().get(0).getEquipmentId()).isEqualTo(10L);
        assertThat(response.getEquipmentItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void updateBooking_approvedBooking_isNotEditable() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(bookingWithStatus(BookingStatus.APPROVED)));

        assertThatThrownBy(() -> bookingService.updateBooking(100L, request(), 1L))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("edit");

        verifyNoInteractions(validationChain);
        verify(bookingRepository, never()).save(any());
    }

    // ---------- updateStatus ----------

    @Test
    void updateStatus_approve_savesAndPublishesEvent() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(bookingWithStatus(BookingStatus.PENDING)));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.updateStatus(100L, BookingStatus.APPROVED, 1L);

        assertThat(response.getStatus()).isEqualTo(BookingStatus.APPROVED);

        ArgumentCaptor<BookingStatusChangedEvent> captor = ArgumentCaptor.forClass(BookingStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getOldStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(captor.getValue().getNewStatus()).isEqualTo(BookingStatus.APPROVED);
        assertThat(captor.getValue().getChangedBy()).isSameAs(requester);
        assertThat(captor.getValue().getBooking().getId()).isEqualTo(100L);
    }

    @Test
    void updateStatus_withoutActor_publishesEventWithNullChangedBy() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(bookingWithStatus(BookingStatus.PENDING)));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        bookingService.updateStatus(100L, BookingStatus.CANCELLED, null);

        ArgumentCaptor<BookingStatusChangedEvent> captor = ArgumentCaptor.forClass(BookingStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getNewStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(captor.getValue().getChangedBy()).isNull();
        verifyNoInteractions(userRepository);
    }

    @Test
    void updateStatus_backToPending_isRejectedBeforeTouchingDatabase() {
        assertThatThrownBy(() -> bookingService.updateStatus(100L, BookingStatus.PENDING, 1L))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(bookingRepository, userRepository, eventPublisher);
    }

    @Test
    void updateStatus_illegalTransition_doesNotSaveOrPublish() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(bookingWithStatus(BookingStatus.COMPLETED)));

        assertThatThrownBy(() -> bookingService.updateStatus(100L, BookingStatus.CANCELLED, null))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("COMPLETED");

        verify(bookingRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void updateStatus_unknownBooking_throwsNotFound() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.updateStatus(999L, BookingStatus.APPROVED, null))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(eventPublisher);
    }
}
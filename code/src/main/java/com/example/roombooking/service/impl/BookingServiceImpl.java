package com.example.roombooking.service.impl;

import com.example.roombooking.domain.entity.Booking;
import com.example.roombooking.domain.entity.BookingEquipment;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.BookingStatus;
import com.example.roombooking.domain.state.BookingContext;
import com.example.roombooking.dto.request.BookingCreateRequest;
import com.example.roombooking.dto.response.BookingResponse;
import com.example.roombooking.event.BookingStatusChangedEvent;
import com.example.roombooking.exception.InvalidStateTransitionException;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.mapper.BookingMapper;
import com.example.roombooking.repository.BookingRepository;
import com.example.roombooking.repository.EquipmentRepository;
import com.example.roombooking.repository.UserRepository;
import com.example.roombooking.service.BookingService;
import com.example.roombooking.service.strategy.BookingRuleStrategyFactory;
import com.example.roombooking.service.validation.BookingValidationChain;
import com.example.roombooking.service.validation.BookingValidationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final BookingValidationChain validationChain;
    private final BookingMapper bookingMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final BookingRuleStrategyFactory ruleStrategyFactory;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              UserRepository userRepository,
                              EquipmentRepository equipmentRepository,
                              BookingValidationChain validationChain,
                              BookingMapper bookingMapper,
                              ApplicationEventPublisher eventPublisher,
                              BookingRuleStrategyFactory ruleStrategyFactory) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.equipmentRepository = equipmentRepository;
        this.validationChain = validationChain;
        this.bookingMapper = bookingMapper;
        this.eventPublisher = eventPublisher;
        this.ruleStrategyFactory = ruleStrategyFactory;
    }

    // ---------- Create ----------

    @Override
    @Transactional
    public BookingResponse createBooking(BookingCreateRequest request, Long requesterId) {
        User requester = findRequesterOrThrow(requesterId);

        request.setBookingId(null); // a new booking must not exclude any existing booking from the checks
        BookingValidationContext context = validate(request, requester);

        User bookedFor = request.getBookingForUserId() == null
                ? requester
                : findUserOrThrow(request.getBookingForUserId());

        Booking booking = bookingMapper.toEntity(request, context.getRoom(), bookedFor);
        attachEquipment(booking, context);

        // Strategy: STANDARD rooms are approved right away, VIP rooms stay PENDING for an admin.
        if (!ruleStrategyFactory.getStrategy(context.getRoom().getRoomType()).requiresApproval()) {
            new BookingContext(booking).approve();
        }

        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    // ---------- Read ----------

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id) {
        return bookingMapper.toResponse(findBookingOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getBookingsByRoom(Long roomId, Pageable pageable) {
        return bookingRepository.findByRoomId(roomId, pageable).map(bookingMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getBookingsByUser(Long userId, Pageable pageable) {
        return bookingRepository.findByUserId(userId, pageable).map(bookingMapper::toResponse);
    }

    // ---------- Update ----------

    @Override
    @Transactional
    public BookingResponse updateBooking(Long id, BookingCreateRequest request, Long requesterId) {
        User requester = findRequesterOrThrow(requesterId);
        Booking booking = findBookingOrThrow(id);

        if (!new BookingContext(booking).isEditable()) {
            throw new InvalidStateTransitionException(booking.getStatus(), "edit");
        }

        request.setBookingId(booking.getId()); // lets the overlap and equipment checks ignore this booking itself
        BookingValidationContext context = validate(request, requester);

        bookingMapper.updateEntity(booking, request, context.getRoom());
        booking.getBookingEquipments().clear(); // orphanRemoval deletes the old rows
        attachEquipment(booking, context);

        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingResponse updateStatus(Long id, BookingStatus targetStatus, Long actorId) {
        Consumer<BookingContext> action = switch (targetStatus) {
            case APPROVED -> BookingContext::approve;
            case REJECTED -> BookingContext::reject;
            case CANCELLED -> BookingContext::cancel;
            case COMPLETED -> BookingContext::complete;
            case PENDING -> throw new IllegalArgumentException("Cannot change a booking back to PENDING.");
        };

        User actor = actorId == null ? null : findUserOrThrow(actorId); // null is logged as "system"
        Booking booking = findBookingOrThrow(id);
        BookingStatus oldStatus = booking.getStatus();

        action.accept(new BookingContext(booking)); // throws InvalidStateTransitionException if not allowed

        Booking saved = bookingRepository.save(booking);
        eventPublisher.publishEvent(
                new BookingStatusChangedEvent(this, saved, oldStatus, saved.getStatus(), actor));

        return bookingMapper.toResponse(saved);
    }

    // ---------- Helpers ----------

    // Chain of Responsibility: permission -> room -> time overlap -> equipment
    private BookingValidationContext validate(BookingCreateRequest request, User requester) {
        BookingValidationContext context = new BookingValidationContext(request, requester);
        validationChain.validate(context);
        return context;
    }

    // Quantities were merged and checked by EquipmentAvailabilityHandler.
    private void attachEquipment(Booking booking, BookingValidationContext context) {
        context.getRequestedEquipmentQuantities().forEach((equipmentId, quantity) ->
                booking.getBookingEquipments().add(new BookingEquipment(
                        null, booking, equipmentRepository.getReferenceById(equipmentId), quantity)));
    }

    private User findRequesterOrThrow(Long requesterId) {
        if (requesterId == null) {
            throw new IllegalArgumentException("X-User-Id header is required.");
        }
        return findUserOrThrow(requesterId);
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: id=" + id));
    }

    private Booking findBookingOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: id=" + id));
    }
}
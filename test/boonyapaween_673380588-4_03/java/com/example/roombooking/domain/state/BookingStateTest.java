package com.example.roombooking.domain.state;

import com.example.roombooking.domain.entity.Booking;
import com.example.roombooking.domain.enums.BookingStatus;
import com.example.roombooking.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * State Pattern: checks every (status, action) pair of BookingContext (5 x 4 = 20 cases).
 * Allowed transitions move the booking to the next status; every other pair throws
 * InvalidStateTransitionException and leaves the status unchanged (same contract for all states = LSP).
 */
class BookingStateTest {

    private static Booking bookingIn(BookingStatus status) {
        return Booking.builder().id(1L).status(status).build();
    }

    private static Consumer<BookingContext> action(String name) {
        return switch (name) {
            case "approve" -> BookingContext::approve;
            case "reject" -> BookingContext::reject;
            case "cancel" -> BookingContext::cancel;
            case "complete" -> BookingContext::complete;
            default -> throw new IllegalArgumentException("Unknown action: " + name);
        };
    }

    static Stream<Arguments> allowedTransitions() {
        return Stream.of(
                Arguments.of(BookingStatus.PENDING, "approve", BookingStatus.APPROVED),
                Arguments.of(BookingStatus.PENDING, "reject", BookingStatus.REJECTED),
                Arguments.of(BookingStatus.PENDING, "cancel", BookingStatus.CANCELLED),
                Arguments.of(BookingStatus.APPROVED, "cancel", BookingStatus.CANCELLED),
                Arguments.of(BookingStatus.APPROVED, "complete", BookingStatus.COMPLETED));
    }

    static Stream<Arguments> rejectedTransitions() {
        return Stream.of(
                Arguments.of(BookingStatus.PENDING, "complete"),
                Arguments.of(BookingStatus.APPROVED, "approve"),
                Arguments.of(BookingStatus.APPROVED, "reject"),
                Arguments.of(BookingStatus.REJECTED, "approve"),
                Arguments.of(BookingStatus.REJECTED, "reject"),
                Arguments.of(BookingStatus.REJECTED, "cancel"),
                Arguments.of(BookingStatus.REJECTED, "complete"),
                Arguments.of(BookingStatus.CANCELLED, "approve"),
                Arguments.of(BookingStatus.CANCELLED, "reject"),
                Arguments.of(BookingStatus.CANCELLED, "cancel"),
                Arguments.of(BookingStatus.CANCELLED, "complete"),
                Arguments.of(BookingStatus.COMPLETED, "approve"),
                Arguments.of(BookingStatus.COMPLETED, "reject"),
                Arguments.of(BookingStatus.COMPLETED, "cancel"),
                Arguments.of(BookingStatus.COMPLETED, "complete"));
    }

    @ParameterizedTest(name = "{0} --{1}--> {2}")
    @MethodSource("allowedTransitions")
    void allowedTransition_movesBookingToNextStatus(BookingStatus from, String actionName, BookingStatus expected) {
        Booking booking = bookingIn(from);

        action(actionName).accept(new BookingContext(booking));

        assertThat(booking.getStatus()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} cannot {1}")
    @MethodSource("rejectedTransitions")
    void rejectedTransition_throwsAndKeepsStatus(BookingStatus from, String actionName) {
        Booking booking = bookingIn(from);
        BookingContext context = new BookingContext(booking);

        assertThatThrownBy(() -> action(actionName).accept(context))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessage("Cannot " + actionName + " a booking in status " + from);

        assertThat(booking.getStatus()).isEqualTo(from);
    }

    @ParameterizedTest
    @EnumSource(BookingStatus.class)
    void onlyPendingBookingIsEditable(BookingStatus status) {
        BookingContext context = new BookingContext(bookingIn(status));

        assertThat(context.isEditable()).isEqualTo(status == BookingStatus.PENDING);
    }

    @Test
    void context_followsChainOfTransitions_andRejectsAfterTerminalState() {
        Booking booking = bookingIn(BookingStatus.PENDING);
        BookingContext context = new BookingContext(booking);

        context.approve();
        context.complete();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(context.isEditable()).isFalse();
        assertThatThrownBy(context::cancel).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void context_wrapsTheSameBookingInstance() {
        Booking booking = bookingIn(BookingStatus.PENDING);

        assertThat(new BookingContext(booking).getBooking()).isSameAs(booking);
    }
}
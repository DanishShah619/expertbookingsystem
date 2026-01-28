package com.danish.patient_booking.service;

import com.danish.patient_booking.dto.SlotUpdateEvent;
import com.danish.patient_booking.model.TimeSlot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcasts a slot status change to all clients subscribed to
     * /topic/experts/{expertId}/slots
     *
     * Called from:
     *  - SeatLockService.lockSlot()    → status = LOCKED
     *  - SeatLockService.releaseLock() → status = AVAILABLE
     *  - BookingService.confirmBooking() → status = BOOKED
     *  - BookingService.handlePaymentFailure() → status = AVAILABLE
     *  - LockExpiryScheduler → status = AVAILABLE
     *
     * @param slot         the TimeSlot that changed
     * @param lockExpiresAt pass the expiry time when LOCKED, null otherwise
     */
    public void broadcastSlotUpdate(TimeSlot slot, LocalDateTime lockExpiresAt) {

        SlotUpdateEvent event = SlotUpdateEvent.builder()
                .slotId(slot.getId())
                .expertId(slot.getExpert().getId())
                .status(slot.getStatus())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .lockExpiresAt(lockExpiresAt)   // null for AVAILABLE/BOOKED — frontend handles this
                .build();

        String destination = "/topic/experts/" + slot.getExpert().getId() + "/slots";

        messagingTemplate.convertAndSend(destination, event);

        log.info("WebSocket broadcast → {} : slotId={} status={}",
                destination, slot.getId(), slot.getStatus());
    }
}

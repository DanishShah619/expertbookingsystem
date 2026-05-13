package com.danish.patient_booking.service;

import com.danish.patient_booking.dto.SlotUpdateEvent;
import com.danish.patient_booking.model.TimeSlot;
import lombok.RequiredArgsConstructor;
import com.danish.patient_booking.util.AppLogger;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private static final AppLogger log = AppLogger.getLogger(WebSocketNotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;


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

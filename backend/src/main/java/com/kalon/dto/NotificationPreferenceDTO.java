package com.kalon.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferenceDTO {
    private boolean orderUpdates;
    private boolean promotionalEmails;
    private boolean stockAlerts;
    private boolean reviewReminders;
    private boolean abandonedCartReminders;
}

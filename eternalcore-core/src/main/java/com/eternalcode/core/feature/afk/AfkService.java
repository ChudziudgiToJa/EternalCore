package com.eternalcode.core.feature.afk;

import com.eternalcode.core.event.EventCaller;
import com.eternalcode.core.injector.annotations.Inject;
import com.eternalcode.core.injector.annotations.component.Service;
import com.eternalcode.core.notice.NoticeService;
import com.eternalcode.core.user.User;
import com.eternalcode.core.user.UserManager;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
class AfkService {

    private final AfkSettings afkSettings;
    private final NoticeService noticeService;
    private final UserManager userManager;
    private final EventCaller eventCaller;

    private final Map<UUID, Afk> afkByPlayer = new HashMap<>();
    private final Map<UUID, Integer> interactionsCount = new HashMap<>();
    private final Map<UUID, Instant> lastInteraction = new HashMap<>();

    @Inject
    AfkService(AfkSettings afkSettings, NoticeService noticeService, UserManager userManager, EventCaller eventCaller) {
        this.afkSettings = afkSettings;
        this.noticeService = noticeService;
        this.userManager = userManager;
        this.eventCaller = eventCaller;
    }

    public void switchAfk(UUID player, AfkReason reason) {
        if (this.isAfk(player)) {
            this.clearAfk(player);
            return;
        }

        this.markAfk(player, reason);
    }

    public Afk markAfk(UUID player, AfkReason reason) {
        Afk afk = new Afk(player, reason, Instant.now());

        this.afkByPlayer.put(player, afk);
        this.eventCaller.callEvent(new AfkSwitchEvent(afk));
        this.sendAfkNotification(player, true);

        return afk;
    }

    public void markInteraction(UUID player) {
        this.lastInteraction.put(player, Instant.now());

        if (!this.isAfk(player)) {
            return;
        }

        int count = this.interactionsCount.getOrDefault(player, 0);
        count++;

        if (count >= this.afkSettings.interactionsCountDisableAfk()) {
            this.clearAfk(player);
            return;
        }

        this.interactionsCount.put(player, count);
    }

    public void clearAfk(UUID player) {
        Afk afk = this.afkByPlayer.remove(player);

        if (afk == null) {
            return;
        }

        this.interactionsCount.remove(player);
        this.lastInteraction.remove(player);
        this.eventCaller.callEvent(new AfkSwitchEvent(afk));
        this.sendAfkNotification(player, false);
    }

    public boolean isAfk(UUID player) {
        return this.afkByPlayer.containsKey(player);
    }

    public boolean isInactive(UUID player) {
        Instant now = Instant.now();
        Instant lastMovement = this.lastInteraction.get(player);

        if (lastMovement != null && Duration.between(lastMovement, now).compareTo(this.afkSettings.getAfkInactivityTime()) >= 0) {
            return true;
        }

        return false;
    }

    private void sendAfkNotification(UUID player, boolean afk) {
        this.noticeService.create()
            .onlinePlayers()
            .player(player)
            .notice(translation -> afk ? translation.afk().afkOn() : translation.afk().afkOff())
            .placeholder("{PLAYER}", this.userManager.getUser(player).map(User::getName))
            .send();
    }

}

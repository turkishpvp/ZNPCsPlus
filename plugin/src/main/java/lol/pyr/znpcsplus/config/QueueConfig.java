package lol.pyr.znpcsplus.config;

import java.util.List;
import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfKey;

import static space.arim.dazzleconf.annote.ConfDefault.*;

/**
 * Messages and behaviour of the {@code queue} npc action, which puts the player into a Phoenix
 * (pxQueue) queue when they click the npc.
 * <p>
 * Every event can send any amount of chat lines plus a title and a subtitle. Empty values are
 * skipped, so a queue can be chat only, title only, or both.
 * <p>
 * Placeholders: {@code {queue}} queue display name, {@code {position}} place in the queue,
 * {@code {size}} amount of players waiting, {@code {player}} the clicking player.
 */
public interface QueueConfig {
    @ConfKey("leave-on-second-click")
    @ConfComments({
            "Should clicking the same npc again take the player out of the queue?",
            "When false, clicking again only shows their current position."
    })
    @DefaultBoolean(true)
    boolean leaveOnSecondClick();

    @ConfKey("title-fade-in")
    @ConfComments("Title fade in time in ticks, used by every queue title")
    @DefaultInteger(5)
    int titleFadeIn();

    @ConfKey("title-stay")
    @ConfComments("Title stay time in ticks")
    @DefaultInteger(40)
    int titleStay();

    @ConfKey("title-fade-out")
    @ConfComments("Title fade out time in ticks")
    @DefaultInteger(10)
    int titleFadeOut();

    @ConfKey("joined-chat")
    @ConfComments("Chat lines sent when the player is added to the queue, empty list for none")
    @DefaultStrings({
            "&8&m----------------------------------------",
            "&a&lSIRAYA GİRDİN",
            "&fSıra: &a{queue}",
            "&fSıradaki yerin: &a{position}&7/&f{size}",
            "&7Çıkmak için NPC'ye tekrar tıkla.",
            "&8&m----------------------------------------"
    })
    List<String> joinedChat();

    @ConfKey("joined-title")
    @DefaultString("&a&lSIRADASIN")
    String joinedTitle();

    @ConfKey("joined-subtitle")
    @DefaultString("&f{queue} &7- &a{position}&7/&f{size}")
    String joinedSubtitle();

    @ConfKey("already-queued-chat")
    @ConfComments("Sent when the player clicks the npc of the queue they are already in and leaving is disabled")
    @DefaultStrings({
            "&eZaten &f{queue} &esırasındasın.",
            "&fSıradaki yerin: &e{position}&7/&f{size}"
    })
    List<String> alreadyQueuedChat();

    @ConfKey("already-queued-title")
    @DefaultString("&e&lSIRADASIN")
    String alreadyQueuedTitle();

    @ConfKey("already-queued-subtitle")
    @DefaultString("&f{queue} &7- &e{position}&7/&f{size}")
    String alreadyQueuedSubtitle();

    @ConfKey("left-chat")
    @ConfComments("Sent when clicking the npc again takes the player out of the queue")
    @DefaultStrings({
            "&c{queue} &fsırasından çıkarıldın."
    })
    List<String> leftChat();

    @ConfKey("left-title")
    @DefaultString("&c&lSIRADAN ÇIKTIN")
    String leftTitle();

    @ConfKey("left-subtitle")
    @DefaultString("&7{queue}")
    String leftSubtitle();

    @ConfKey("switched-chat")
    @ConfComments("Sent when the player was waiting in another queue and got moved to this one")
    @DefaultStrings({
            "&aSıran değişti: &f{queue}",
            "&fSıradaki yerin: &a{position}&7/&f{size}"
    })
    List<String> switchedChat();

    @ConfKey("switched-title")
    @DefaultString("&a&lSIRA DEĞİŞTİ")
    String switchedTitle();

    @ConfKey("switched-subtitle")
    @DefaultString("&f{queue} &7- &a{position}&7/&f{size}")
    String switchedSubtitle();

    @ConfKey("unavailable-chat")
    @ConfComments("Sent when Phoenix / pxQueue is not installed or the queue name does not exist")
    @DefaultStrings({
            "&cSıra sistemi şu an kullanılamıyor."
    })
    List<String> unavailableChat();

    @ConfKey("unavailable-title")
    @DefaultString("")
    String unavailableTitle();

    @ConfKey("unavailable-subtitle")
    @DefaultString("")
    String unavailableSubtitle();
}

package top.likoslupus.cellulosesz.core.i18n;

import java.util.LinkedHashMap;
import java.util.Map;

final class MessagingMessages {

    private MessagingMessages() {
    }

    static Map<String, String> english() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.messaging.console-disabled", "<red>Console disabled.");
        messages.put("commands.messaging.mail-cleared", "<primary>Cleared <secondary>{count}<primary> mail message(s).");
        messages.put("commands.messaging.mail-deleted", "<primary>Deleted mail <secondary>{id}<primary>.");
        messages.put("commands.messaging.mail-empty", "<red>Mail empty.");
        messages.put("commands.messaging.mail-invalid-duration", "<red>Mail invalid duration.");
        messages.put("commands.messaging.mail-invalid-id", "<red>Mail invalid id.");
        messages.put("commands.messaging.mail-mark-read-failed", "<red>Mail mark read failed.");
        messages.put("commands.messaging.mail-not-found", "<red>Mail not found.");
        messages.put("commands.messaging.mail-page", "<primary>Mail page <secondary>{page}<primary>/<secondary>{pages}<primary>; unread <secondary>{unread}<primary>.{entries}");
        messages.put("commands.messaging.mail-sent", "<primary>Mail sent to <secondary>{player}<primary>.");
        messages.put("commands.messaging.mail-sent-all", "<primary>Mail sent to <secondary>{count}<primary> player(s).");
        messages.put("commands.messaging.mail-storage-failed", "<red>Mail storage failed.");
        messages.put("commands.messaging.mail-unread", "<primary>Unread mail: <secondary>{count}<primary>.");
        messages.put("commands.messaging.mail-usage", "<red>Usage: <secondary>{usage}");
        messages.put("commands.messaging.preference-save-failed", "<red>Preference save failed.");
        messages.put("commands.messaging.socialspy-player-required", "<primary>Socialspy player required.");
        messages.put("service.messaging.empty-message", "<red>Empty message.");
        messages.put("service.messaging.persistence-failed", "<red>Persistence failed.");
        messages.put("commands.messaging.mail-sendall-too-many", "<red>Mail sendall targets <secondary>{count}<red> players; the configured maximum is <secondary>{maximum}<red>.");
        return Map.copyOf(messages);
    }

    static Map<String, String> chinese() {
        var messages = new LinkedHashMap<String, String>();
        messages.put("commands.messaging.console-disabled", "<red>操作失败：Console disabled。");
        messages.put("commands.messaging.mail-cleared", "<primary>已清除 <secondary>{count}<primary> 封邮件。");
        messages.put("commands.messaging.mail-deleted", "<primary>已删除邮件 <secondary>{id}<primary>。");
        messages.put("commands.messaging.mail-empty", "<red>操作失败：Mail empty。");
        messages.put("commands.messaging.mail-invalid-duration", "<red>操作失败：Mail invalid duration。");
        messages.put("commands.messaging.mail-invalid-id", "<red>操作失败：Mail invalid id。");
        messages.put("commands.messaging.mail-mark-read-failed", "<red>操作失败：Mail mark read failed。");
        messages.put("commands.messaging.mail-not-found", "<red>操作失败：Mail not found。");
        messages.put("commands.messaging.mail-page", "<primary>邮件第 <secondary>{page}<primary>/<secondary>{pages}<primary> 页；未读 <secondary>{unread}<primary> 封。{entries}");
        messages.put("commands.messaging.mail-sent", "<primary>邮件已发送给 <secondary>{player}<primary>。");
        messages.put("commands.messaging.mail-sent-all", "<primary>邮件已发送给 <secondary>{count}<primary> 名玩家。");
        messages.put("commands.messaging.mail-storage-failed", "<red>操作失败：Mail storage failed。");
        messages.put("commands.messaging.mail-unread", "<primary>未读邮件：<secondary>{count}<primary>。");
        messages.put("commands.messaging.mail-usage", "<red>用法：<secondary>{usage}");
        messages.put("commands.messaging.preference-save-failed", "<red>操作失败：Preference save failed。");
        messages.put("commands.messaging.socialspy-player-required", "<primary>Socialspy player required。");
        messages.put("service.messaging.empty-message", "<red>操作失败：Empty message。");
        messages.put("service.messaging.persistence-failed", "<red>操作失败：Persistence failed。");
        messages.put("commands.messaging.mail-sendall-too-many", "<red>群发邮件目标为 <secondary>{count}<red> 名玩家，超过配置上限 <secondary>{maximum}<red>。");
        return Map.copyOf(messages);
    }

}

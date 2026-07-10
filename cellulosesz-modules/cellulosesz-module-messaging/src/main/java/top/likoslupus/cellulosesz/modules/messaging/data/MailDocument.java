package top.likoslupus.cellulosesz.modules.messaging.data;

import top.likoslupus.cellulosesz.api.messaging.MailMessage;

import java.util.ArrayList;
import java.util.List;

public final class MailDocument {

    public int schema = 1;
    public List<MailMessage> messages = new ArrayList<>();

}

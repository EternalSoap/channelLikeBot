package com.goldner.telegram.channelLikeBot;

import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

/**
 * Created by fran on 06.04.17..
 */
public class channelLikeBot extends TelegramLongPollingBot {
    public void onUpdateReceived(Update update) {

    }

    @Override
    public String getBotUsername() {
        return "channelLike_bot";
    }

    @Override
    public String getBotToken() {
        return "*************************************";
    }
}

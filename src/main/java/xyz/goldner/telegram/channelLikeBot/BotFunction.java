package xyz.goldner.telegram.channelLikeBot;

import org.telegram.telegrambots.api.methods.groupadministration.GetChatAdministrators;

import org.telegram.telegrambots.api.objects.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by fran on 07.04.17..
 */

/*
todo: it seems channelPost is another type of message, add functions to handle them
 */

public class BotFunction{

    public enum PrivacyMode {MEMBER, ADMINISTRATOR, CREATOR}

    public enum ChatType {PRIVATE, GROUP, SUPERGROUP, CHANNEL, ANY}
    public enum MessageType {TEXT, PHOTO, DOCUMENT, CALLBACK, LOCATION}

    private final TelegramLongPollingBot telegramLongPollingBot;

    private final BiConsumer<Update, Object> function;
    private final String command;
    private final PrivacyMode privacyMode;
    private final ChatType chatType;
    private final MessageType messageType;
    private final MessageType replyType;
    private final boolean isReply;
    private final boolean isCallback;

    private BotFunction(TelegramLongPollingBot telegramLongPollingBot, BiConsumer<Update, Object> function, String command, PrivacyMode privacyMode, ChatType chatType, MessageType messageType, MessageType replyType, boolean isReply, boolean isCallback) {
        this.telegramLongPollingBot = telegramLongPollingBot;
        this.function = function;
        this.command = command;
        this.privacyMode = privacyMode;
        this.chatType = chatType;
        this.messageType = messageType;
        this.replyType = replyType;
        this.isReply = isReply;
        this.isCallback = isCallback;

    }

    public BiConsumer<Update, Object> getFunction() {
        return function;
    }

    public String getCommand() {
        return command;
    }

    public PrivacyMode getPrivacyMode() {
        return privacyMode;
    }

    public ChatType getChatType() {
        return chatType;
    }

    public MessageType getReplyType() {
        return replyType;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public boolean isReply() {
        return isReply;
    }

    public boolean isCallback() {return isCallback;}

    private boolean testPrivacy(Update update){


        Message message;
        Chat chat;
        long userID;
        long chatID;

        if(this.isCallback){

            message = update.getCallbackQuery().getMessage();
            chat = message.getChat();
            userID = update.getCallbackQuery().getFrom().getId();
            chatID = chat.getId();

        }else{

            message = update.getMessage();
            chat = message.getChat();
            userID = update.getMessage().getFrom().getId();
            chatID = chat.getId();

        }

        if (chat.isUserChat()
                && (this.chatType == ChatType.ANY || this.chatType == ChatType.PRIVATE)) {
            return true;
        }

        ChatMember creator = null;
        List<ChatMember> adminList = new ArrayList<>();


        try {
            adminList = telegramLongPollingBot.getChatAdministrators(new GetChatAdministrators().setChatId(chatID));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }


        for (ChatMember chatMember :
                adminList) {
            if (chatMember.getStatus().equals("creator")) {
                creator = chatMember;
            }
        }


        switch (this.privacyMode) {
            case MEMBER:
                return true;
            case ADMINISTRATOR:
                return adminList.stream().filter(member -> member.getUser().getId() == userID).count() == 1;
            case CREATOR:
                return creator != null && userID == creator.getUser().getId();
            default:
                return false;
        }


    }

    private boolean testChat(Update update){

        Chat chat;

        if(this.isCallback){
            chat = update.getCallbackQuery().getMessage().getChat();
        }else{
            chat = update.getMessage().getChat();
        }

        switch (this.chatType){
            case ANY: return true;
            case GROUP:
                return chat.isGroupChat();
            case PRIVATE:
                return chat.isUserChat();
            case CHANNEL:
                return chat.isChannelChat();
            case SUPERGROUP:
                return chat.isSuperGroupChat();
            default: return false;
        }

    }

    private boolean testType(Update update){

        Message message;
        String msgCommand;

        if(this.isCallback){
            message = update.getCallbackQuery().getMessage();
            msgCommand = update.getCallbackQuery().getData();
        }else{
            message = update.getMessage();
            msgCommand = message.getText();
        }

        switch (this.messageType) {
            case CALLBACK:
                return testCommand(msgCommand);
            case TEXT:
                return message.hasText() && testCommand(msgCommand);
            case PHOTO:
                return message.hasPhoto();
            case DOCUMENT:
                return message.hasDocument();
            case LOCATION:
                return message.hasLocation();
            default:
                return false;
        }

    }

    private boolean testCommand(String msgCommand) {

        return msgCommand.equals(this.command);

    }

    private boolean testReply(Update update) {

        return !this.isReply || testReplyType(update);

    }

    private boolean testReplyType(Update update) {


        Message message = update.getMessage().getReplyToMessage();
        if(message == null) return false;

        switch (this.replyType){
            case TEXT:
                return message.hasText();
            case PHOTO:
                return message.hasPhoto();
            case DOCUMENT:
                return message.hasDocument();
            case LOCATION:
                return message.hasLocation();
            default: return false;
        }

    }

    private boolean testCallback(Update update){
        return update.hasCallbackQuery() == this.isCallback;
    }

    private boolean runTests(Update update){


        return testCallback(update) && testChat(update) && testPrivacy(update) && testType(update) && testReply(update);



    }

    void tryRun(Update update){

        Message message;

        if(update.hasCallbackQuery() && this.isCallback){
            message = update.getCallbackQuery().getMessage();
        }else{
            message = update.getMessage();
        }


        if(runTests(update)){
            function.accept(update, message.getChatId());
        }

    }




    static class BotFunctionBuilder{


        private TelegramLongPollingBot telegramLongPollingBot;
        private BiConsumer<Update, Object> function;
        private String command;
        private PrivacyMode privacyMode = PrivacyMode.MEMBER;
        private ChatType chatType = ChatType.ANY;
        private MessageType messageType = MessageType.TEXT;
        private MessageType replyType = MessageType.TEXT;
        private boolean isReply = false;
        private boolean isCallback = false;

        BotFunctionBuilder setTelegramLongPollingBot(TelegramLongPollingBot telegramLongPollingBot) {
            this.telegramLongPollingBot = telegramLongPollingBot;
            return this;
        }

        BotFunctionBuilder setFunction(BiConsumer<Update, Object> function) {
            this.function = function;
            return this;
        }

        BotFunctionBuilder setCommand(String command) {
            this.command = command;
            return this;
        }

        BotFunctionBuilder setPrivacyMode(PrivacyMode privacyMode) {
            this.privacyMode = privacyMode;
            return this;
        }

        BotFunctionBuilder setChatType(ChatType chatType) {
            this.chatType = chatType;
            return this;
        }

        BotFunctionBuilder setMessageType(MessageType messageType) {
            this.messageType = messageType;
            return this;
        }

        BotFunctionBuilder setReplyType(MessageType replyType) {
            this.replyType = replyType;
            return this;
        }

        BotFunctionBuilder setIsReply(boolean isReply) {
            this.isReply = isReply;
            return this;
        }

        BotFunctionBuilder setIsCallback(boolean isCallback){
            this.isCallback = isCallback;
            return this;
        }

        BotFunction createBotFunction() {
            return new BotFunction(telegramLongPollingBot, function, command, privacyMode, chatType, messageType, replyType, isReply, isCallback);
        }
    }

}






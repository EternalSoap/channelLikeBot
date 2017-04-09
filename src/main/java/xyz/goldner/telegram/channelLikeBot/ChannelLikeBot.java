package xyz.goldner.telegram.channelLikeBot;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.vdurmont.emoji.EmojiParser;
import org.telegram.telegrambots.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.PhotoSize;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Created by fran on 06.04.17..
 *
 *
 *  todo replace functionList with another mechanism of storing functions, preferably with auto-add feature (createBotFunction)
 */


public class ChannelLikeBot extends TelegramLongPollingBot {

    private static final String thumbsup = EmojiParser.parseToUnicode(":thumbsup:");
    private static final String thumbsdown = EmojiParser.parseToUnicode(":thumbsdown:");

    private String channelId;

    private Database database;
    
    private List<BotFunction> functionList = new ArrayList<>();

    ChannelLikeBot(Database database, String channelId){

        super();
        this.database = database;
        this.channelId = channelId;

        setupFunctions();

    }

    private void setupFunctions() {

        BotFunction start = new BotFunction.BotFunctionBuilder()
                    .setChatType(BotFunction.ChatType.ANY)
                    .setCommand("/start")
                    .setIsReply(false)
                    .setMessageType(BotFunction.MessageType.TEXT)
                    .setTelegramLongPollingBot(this)
                    .setPrivacyMode(BotFunction.PrivacyMode.MEMBER)
                    .setFunction((u,f) -> {
                        try {
                           sendMessage(new SendMessage().setText("Henlo " + u.getMessage().getFrom().getFirstName()).setChatId((long) f));
                       } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    })
                    .createBotFunction();

        BotFunction replyToPic = new BotFunction.BotFunctionBuilder()
                    .setIsReply(true)
                    .setReplyType(BotFunction.MessageType.PHOTO)
                    .setMessageType(BotFunction.MessageType.TEXT)
                    .setChatType(BotFunction.ChatType.SUPERGROUP)
                    .setPrivacyMode(BotFunction.PrivacyMode.ADMINISTRATOR)
                    .setCommand("/post")
                    .setFunction((u,f)-> keyboardStuff(u))
                    .setTelegramLongPollingBot(this)
                    .createBotFunction();

        BotFunction getChannel = new BotFunction.BotFunctionBuilder()
                    .setCommand("/getChannel")
                    .setTelegramLongPollingBot(this)
                    .setFunction((u, f) -> {
                        try {
                            sendMessage(new SendMessage().setText(channelId).setChatId(u.getMessage().getChatId()));
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    })
                    .createBotFunction();

        BotFunction like = new BotFunction.BotFunctionBuilder()
                    .setIsCallback(true)
                    .setCommand("like")
                    .setMessageType(BotFunction.MessageType.CALLBACK)
                    .setFunction((u,f) -> callbackLike(u))
                    .setTelegramLongPollingBot(this)
                    .createBotFunction();

        BotFunction dislike = new BotFunction.BotFunctionBuilder()
                    .setIsCallback(true)
                    .setCommand("dislike")
                    .setMessageType(BotFunction.MessageType.CALLBACK)
                    .setFunction((u,f) -> callbackLike(u))
                    .setTelegramLongPollingBot(this)
                    .createBotFunction();

        functionList.add(start);
        functionList.add(replyToPic);
        functionList.add(getChannel);
        functionList.add(like);
        functionList.add(dislike);


    }

    private void callbackLike(Update update) {

        String data = update.getCallbackQuery().getData();

        Message originalMessage = update.getCallbackQuery().getMessage();

        long userID = update.getCallbackQuery().getFrom().getId();

        List<PhotoSize> photoList = originalMessage.getPhoto();
        String fileID = photoList.stream().sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
                .findFirst().orElse(null).getFileId();

        AnswerCallbackQuery answer = new AnswerCallbackQuery()
                .setCallbackQueryId(update.getCallbackQuery().getId())
                .setShowAlert(false);

        if(vote(fileID,userID, data)) {


            InlineKeyboardMarkup keyboard = getKeyboard(fileID);

            EditMessageReplyMarkup edit = new EditMessageReplyMarkup()
                    .setChatId(originalMessage.getChatId())
                    .setMessageId(originalMessage.getMessageId())
                    .setReplyMarkup(keyboard);



            try {
                editMessageReplyMarkup(edit);
                answerCallbackQuery(answer);

            } catch (TelegramApiException e) {
                e.printStackTrace();
            }

        }else{
            try {
                answerCallbackQuery(answer);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean vote(String fileID, long userID, String data) {


        RedisCommands<String, String> redisCommands = database.getRedis();

        String oldData = redisCommands.hget(fileID, Long.toString(userID));

        if(oldData != null && oldData.equals(data)){
            return false;
        }else {

            redisCommands.hset(fileID, Long.toString(userID), data);
            return true;
        }
    }

    private void keyboardStuff(Update update) {


        Message replyMessage = update.getMessage().getReplyToMessage();

        List<PhotoSize> photoList = replyMessage.getPhoto();
        String fileID = photoList.stream().sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
                .findFirst().orElse(null).getFileId();

        // create inline keyboard

        InlineKeyboardMarkup keyboard = getKeyboard(fileID);

        // create channel post


        SendPhoto channelPost = new SendPhoto()
                .setChatId(channelId)
                .setPhoto(fileID)
                .setReplyMarkup(keyboard);

        try {
            sendPhoto(channelPost);

        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    private InlineKeyboardMarkup getKeyboard (String photoID){

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        int likes = 0;
        int dislikes = 0;

        RedisCommands<String,String> redisCommands = database.getRedis();

        Map<String,String> votes = redisCommands.hgetall(photoID);

        for (String key :
                votes.keySet()) {
            if (votes.get(key).equals("like")) {
                likes++;
            }else if (votes.get(key).equals("dislike")){
                dislikes++;
            }
            }

        row.add(new InlineKeyboardButton().setText(thumbsup + " " + Integer.toString(likes)).setCallbackData("like"));
        row.add(new InlineKeyboardButton().setText(thumbsdown + " " + Integer.toString(dislikes)).setCallbackData("dislike"));
        rows.add(row);

        keyboard.setKeyboard(rows);

        return keyboard;

    }

    public void onUpdateReceived(Update update) {


        for (BotFunction bf :
                functionList) {
            bf.tryRun(update);
        }



    }


    @Override
    public String getBotUsername() {
        return "channelLike_bot";
    }

    @Override
    public String getBotToken() {
        return "";
    }
}

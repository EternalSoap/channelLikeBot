package xyz.goldner.telegram.channelLikeBot;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.util.ArrayList;
import java.util.List;


public class App
{

    private static final String CHANNEL_ID = "@aesthy";

    private static final int CHANNEL_LIKE_BOT_DB = 1;

    private static List<Database> databaseList = new ArrayList<>();

    public static void main( String[] args )
    {

        ApiContextInitializer.init();

        TelegramBotsApi botsApi = new TelegramBotsApi();

        try{
            Database database = new Database(CHANNEL_LIKE_BOT_DB);
            databaseList.add(database);
            botsApi.registerBot(new ChannelLikeBot(databaseList.get(0), CHANNEL_ID));
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> databaseList.stream().forEach(database -> database.disconnect())));

    }

}

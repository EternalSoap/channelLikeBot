package xyz.goldner.telegram.channelLikeBot;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.sun.org.apache.regexp.internal.REDebugCompiler;

/**
 * Created by fran on 06.04.17..
 */
public class Database {

    private static RedisClient redisClient;
    private static StatefulRedisConnection<String,String> connection;
    private static RedisCommands<String,String> syncCommands;


    Database(int databaseNumber){
        RedisURI redisURI = RedisURI.Builder.redis("localhost").withDatabase(databaseNumber).build();
        redisClient = RedisClient.create(redisURI);
        connection = redisClient.connect();
        syncCommands = connection.sync();
    }

    RedisCommands getRedis(){
        return syncCommands;
    }

    public void disconnect(){
        connection.close();
        redisClient.shutdown();
    }



}

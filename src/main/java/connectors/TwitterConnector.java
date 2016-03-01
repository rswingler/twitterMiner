package connectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.endpoint.StatusesSampleEndpoint;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.processor.HosebirdMessageProcessor;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by bobbyswingler on 2/25/16.
 */
public class TwitterConnector
{
    private Client hosebirdClient;
    BlockingQueue<String> msgQueue;
    BlockingQueue<Event> eventQueue;
    ObjectMapper mapper;

    public TwitterConnector(){
        msgQueue = new LinkedBlockingQueue<String>(100000);
        eventQueue = new LinkedBlockingQueue<Event>(1000);
        mapper = new ObjectMapper();
    }

    public void connect(){
        Authentication auth = buildHosebirdAuth();
        hosebirdClient = buildHosebirdClient(auth);
        hosebirdClient.connect();
    }

    public void disconnect(){
        hosebirdClient.stop();
    }

    public void getStream(){
//        Runnable twitterStream = new Runnable() {
//            public void run() {
//                while (true) {
//                    //System.out.println("clientLoop");
//                    String msg = null;
//                    try {
//                        msg = msgQueue.take();
//                        System.out.println("MESSAGE: " + msg);
//                    }
//                    catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        };
//        twitterStream.run();

        // Do whatever needs to be done with messages
        for (int msgRead = 0; msgRead < 1000; msgRead++) {
            if (hosebirdClient.isDone()) {
                System.out.println("Client connection closed unexpectedly: ");
                break;
            }

            String msg = null;
            try {
                msg = msgQueue.poll(5, TimeUnit.SECONDS);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (msg == null) {
                System.out.println("Did not receive a message in 5 seconds");
            } else {

                try {
                    JsonNode tweet = mapper.readTree(msg);
                    long id = tweet.get("id").asLong();
                    String textBody = tweet.get("text").asText().replace("\n", "").replace("\r", "");
                    int retweetCount = tweet.get("retweet_count").asInt();
                    int favoriteCount = tweet.get("favorite_count").asInt();
                    String language = tweet.get("lang").asText();
                    String delim = "*****";

                    if (language.equals("en")){
                        System.out.println(id + delim + textBody + delim + retweetCount + delim + favoriteCount + delim);
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }

                //System.out.println(msg);
            }
        }

        hosebirdClient.stop();

        // Print some stats
        System.out.printf("The client read %d messages!\n", hosebirdClient.getStatsTracker().getNumMessages());
    }



    private Authentication buildHosebirdAuth(){
        String CONSUMER_KEY = "";
        String CONSUMER_SECRET = "";
        String ACCESS_TOKEN = "";
        String ACCESS_TOKEN_SECRET = "";

        File propertiesFile = new File("project.properties");

        try {
            Scanner scanner = new Scanner(propertiesFile);
            String props = "";
            while (scanner.hasNext()){
                props += scanner.next();
            }

            JsonNode properties = mapper.readTree(props);

            CONSUMER_KEY = properties.get("CONSUMER_KEY").asText();
            CONSUMER_SECRET = properties.get("CONSUMER_SECRET").asText();
            ACCESS_TOKEN = properties.get("ACCESS_TOKEN").asText();
            ACCESS_TOKEN_SECRET = properties.get("ACCESS_TOKEN_SECRET").asText();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // These secrets should be read from a config file
        //Authentication hosebirdAuth = new OAuth1(CONSUMER_KEY, CONSUMER_SECRET, ACCESS_TOKEN, ACCESS_TOKEN_SECRET);
        return new OAuth1(CONSUMER_KEY, CONSUMER_SECRET, ACCESS_TOKEN, ACCESS_TOKEN_SECRET);
    }

    private Client buildHosebirdClient(Authentication hosebirdAuth){

        /** Set up your blocking queues: Be sure to size these properly based on expected TPS of your stream */
        //BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(100000);
        //BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<Event>(1000);

        /** Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();

        //Twitter User IDs (follows)
        long appleNews = 37019708;
        long macRumors = 14861285;
        //long timCook = 1636590253;
        //long stevewoz = 22938914;
        long nineToFiveMac = 15944436;
        long cultOfMac = 9688342;
        long macTrast = 16711478;
        long appleStreem = 15403842;
        long appleInsider = 20542450;


        // Optional: set up some followings and track terms
        List<Long> followings = Lists.newArrayList(appleNews, macRumors, nineToFiveMac, cultOfMac, macTrast, appleStreem, appleInsider);
        List<String> terms = Lists.newArrayList("apple");
        //hosebirdEndpoint.followings(followings);
        hosebirdEndpoint.trackTerms(terms);

        ClientBuilder builder = new ClientBuilder()
                .name("Hosebird-Client-01")                              // optional: mainly for the logs
                .hosts(hosebirdHosts)
                .authentication(hosebirdAuth)
                .endpoint(hosebirdEndpoint)
                .processor(new StringDelimitedProcessor(msgQueue))
                .eventMessageQueue(eventQueue);                          // optional: use this if you want to process client events

        //Client hosebirdClient = builder.build();
        return builder.build();
    }

}

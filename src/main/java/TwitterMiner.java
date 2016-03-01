import connectors.TwitterConnector;

/**
 * Created by bobbyswingler on 2/25/16.
 */
public class TwitterMiner
{
    public static void main(String[] args){
        TwitterConnector twitter = new TwitterConnector();
        twitter.connect();
        twitter.getStream();
    }
}

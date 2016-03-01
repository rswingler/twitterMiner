package connectors;

import com.twitter.hbc.core.StatsReporter;
import com.twitter.hbc.core.endpoint.StreamingEndpoint;

/**
 * Created by bobbyswingler on 2/25/16.
 */
public class HoseBirdClient implements com.twitter.hbc.core.Client
{

    @Override
    public void connect() {

    }

    @Override
    public void reconnect() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void stop( int waitMillis ) {

    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public StreamingEndpoint getEndpoint() {
        return null;
    }

    @Override
    public StatsReporter.StatsTracker getStatsTracker() {
        return null;
    }
}

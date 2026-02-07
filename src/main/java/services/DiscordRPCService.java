package services;

import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.activity.Activity;

import java.time.Instant;
import java.util.concurrent.*;

public class DiscordRPCService {
    private Core core;
    private ScheduledExecutorService callBackExecutor;

    /**
     * Standard service needed for Discord Social SDK's RPC.
     * @param clientId - Client ID from Discord (i.e 123456789L)
     * */

    public void start(long clientId) {
        CreateParams params = new CreateParams();
        params.setClientID(clientId);
        params.setFlags(CreateParams.getDefaultFlags());
        core = new Core(params);
        callBackExecutor = Executors.newSingleThreadScheduledExecutor();
        callBackExecutor.scheduleAtFixedRate(
                core::runCallbacks,
                0,
                10,
                TimeUnit.MILLISECONDS
        );
    }

    public void initRpc() {
        try(Activity activity = new Activity()) {
            activity.setDetails("Reading with Yomikomu");
            activity.timestamps().setStart(Instant.now());
            core.activityManager().updateActivity(activity);
        }
    }
    public void stop() {
        callBackExecutor.shutdownNow();
        core.close();
    }

}

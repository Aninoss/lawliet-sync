package syncserver.events;

import org.json.JSONObject;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

import java.io.File;
import java.util.Calendar;

@SyncServerEvent(event = "DEV_VOTES_PRESENT")
public class OnDevVotesPresent implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);
        String filename = System.getenv("DEVVOTES_DIR") + "/" + month + "_" + year + "_en.properties";

        JSONObject responseJson = new JSONObject();
        responseJson.put("present", new File(filename).exists());
        return responseJson;
    }

}

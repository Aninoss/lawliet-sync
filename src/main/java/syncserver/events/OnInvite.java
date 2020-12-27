package syncserver.events;

import core.InviteTypes;
import mysql.modules.invitetypeusages.DBInviteTypeUsages;
import org.json.JSONObject;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;
import java.util.Arrays;

@SyncServerEvent(event = "INVITE")
public class OnInvite implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject dataJson) {
        String typeString = dataJson.getString("type");

        Arrays.stream(InviteTypes.values())
                .filter(type -> type.name().equalsIgnoreCase(typeString))
                .forEach(type -> DBInviteTypeUsages.getInstance().insertInvite(type));

        return null;
    }

}
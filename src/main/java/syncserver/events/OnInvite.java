package syncserver.events;

import java.util.Arrays;
import core.InviteTypes;
import mysql.modules.invitetypeusages.DBInviteTypeUsages;
import org.json.JSONObject;
import syncserver.ClientTypes;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "INVITE")
public class OnInvite implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject dataJson) {
        if (socketId.equals(ClientTypes.WEB)) {
            String typeString = dataJson.getString("type");

            Arrays.stream(InviteTypes.values())
                    .filter(type -> type.name().equalsIgnoreCase(typeString))
                    .forEach(type -> DBInviteTypeUsages.getInstance().insertInvite(type));
        }
        return null;
    }

}
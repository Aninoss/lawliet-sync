package syncserver.events;

import java.util.Arrays;
import core.InviteTypes;
import mysql.modules.invitetypeusages.DBInviteTypeUsages;
import org.json.JSONObject;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "INVITE")
public class OnInvite implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        String typeString = jsonObject.getString("type");
        Arrays.stream(InviteTypes.values())
                .filter(type -> type.name().equalsIgnoreCase(typeString))
                .forEach(type -> DBInviteTypeUsages.getInstance().insertInvite(type));
        return null;
    }

}
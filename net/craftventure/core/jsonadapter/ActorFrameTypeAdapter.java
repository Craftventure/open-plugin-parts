package net.craftventure.core.jsonadapter;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.craftventure.core.npc.actor.ActorFrame;
import net.craftventure.core.npc.actor.action.*;

import java.lang.reflect.Type;


public class ActorFrameTypeAdapter implements JsonSerializer<ActorFrame>, JsonDeserializer<ActorFrame> {
    @Override
    public ActorFrame deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        long time = jsonObject.get("time").getAsLong();
        int actionType = jsonObject.get("type").getAsInt();
        if (actionType == ActorAction.Type.ANIMATED_MOVE ||
                actionType == ActorAction.Type.MOVE) {
            if (actionType == ActorAction.Type.MOVE) {
//                Logger.info("MOVE!");
            }
            return new ActorFrame<>(time, (ActorAction) jsonDeserializationContext.deserialize(jsonObject.get("data"), new TypeToken<ActionAnimatedMove>() {
            }.getType()));
        } else if (actionType == ActorAction.Type.EQUIPMENT) {
            return new ActorFrame<>(time, (ActorAction) jsonDeserializationContext.deserialize(jsonObject.get("data"), new TypeToken<ActionEquipment>() {
            }.getType()));
        } else if (actionType == ActorAction.Type.ARMORSTAND_POSE) {
            return new ActorFrame<>(time, (ActorAction) jsonDeserializationContext.deserialize(jsonObject.get("data"), new TypeToken<ActionArmorStandPose>() {
            }.getType()));
        } else if (actionType == ActorAction.Type.STATE_FLAG) {
            return new ActorFrame<>(time, (ActorAction) jsonDeserializationContext.deserialize(jsonObject.get("data"), new TypeToken<ActionStateFlag>() {
            }.getType()));
        } else if (actionType == ActorAction.Type.DOUBLE_SETTING) {
            return new ActorFrame<>(time, (ActorAction) jsonDeserializationContext.deserialize(jsonObject.get("data"), new TypeToken<ActionDoubleSetting>() {
            }.getType()));
        }

        return null;
    }

    @Override
    public JsonElement serialize(ActorFrame actorFrame, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("time", actorFrame.getTime());
        jsonObject.addProperty("type", actorFrame.getAction().getActionTypeId());
        jsonObject.add("data", jsonSerializationContext.serialize(actorFrame.getAction()));
        return jsonObject;
    }
}

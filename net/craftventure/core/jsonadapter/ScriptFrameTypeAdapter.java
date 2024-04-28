package net.craftventure.core.jsonadapter;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.craftventure.core.ktx.util.Logger;
import net.craftventure.core.script.action.*;

import java.lang.reflect.Type;


public class ScriptFrameTypeAdapter implements JsonSerializer<ScriptActionFrame>, JsonDeserializer<ScriptActionFrame> {
    @Override
    public ScriptActionFrame deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        long time = jsonObject.get("time").getAsLong();
        int actionType = jsonObject.get("type").getAsInt();
        if (actionType == ScriptAction.Type.PLACE_SCHEMATIC) {
            return new ScriptActionFrame<>(time, (PlaceSchematicAction) jsonDeserializationContext.deserialize(jsonObject.get("data"), new TypeToken<PlaceSchematicAction>() {
            }.getType()));
        } else if (actionType == ScriptAction.Type.BLOCK_CHANGE) {
            return new ScriptActionFrame<>(time, (BlockChangeAction) jsonDeserializationContext.deserialize(jsonObject.get("data"), new TypeToken<BlockChangeAction>() {
            }.getType()));
        } else if (actionType == ScriptAction.Type.STRIKE_LIGHTNING) {
            return new ScriptActionFrame<>(time, (StrikeLightningAction) jsonDeserializationContext.deserialize(jsonObject.get("data"), new TypeToken<StrikeLightningAction>() {
            }.getType()));
        } else if (actionType == ScriptAction.Type.CHAT) {
            return new ScriptActionFrame<>(time, (ChatAction) jsonDeserializationContext.deserialize(jsonObject.get("data"), new TypeToken<ChatAction>() {
            }.getType()));
        } else if (actionType == ScriptAction.Type.PLAY_SOUND) {
            return new ScriptActionFrame<>(time, (PlaySoundAction) jsonDeserializationContext.deserialize(jsonObject.get("data"), new TypeToken<PlaySoundAction>() {
            }.getType()));
        } else if (actionType == ScriptAction.Type.POTION_EFFECT) {
            return new ScriptActionFrame<>(time, (PotionEffectAction) jsonDeserializationContext.deserialize(jsonObject.get("data"), new TypeToken<PotionEffectAction>() {
            }.getType()));
        } else if (actionType == ScriptAction.Type.BLOCK_ACTION) {
            return new ScriptActionFrame<>(time, (BlockActionAction) jsonDeserializationContext.deserialize(jsonObject.get("data"), new TypeToken<BlockActionAction>() {
            }.getType()));
        } else if (actionType == ScriptAction.Type.FALLING_AREA) {
            return new ScriptActionFrame<>(time, (FallingAreaAction) jsonDeserializationContext.deserialize(jsonObject.get("data"), new TypeToken<FallingAreaAction>() {
            }.getType()));
        } else if (actionType == ScriptAction.Type.EFFECT) {
            return new ScriptActionFrame<>(time, (EffectAction) jsonDeserializationContext.deserialize(jsonObject.get("data"), new TypeToken<EffectAction>() {
            }.getType()));
        } else if (actionType == ScriptAction.Type.LIGHTING) {
            return new ScriptActionFrame<>(time, (LightingEffect) jsonDeserializationContext.deserialize(jsonObject.get("data"), new TypeToken<LightingEffect>() {
            }.getType()));
        } else {
            Logger.severe("ActionType not found " + actionType);
        }

        return null;
    }

    @Override
    public JsonElement serialize(ScriptActionFrame scriptActionFrame, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("time", scriptActionFrame.getTime());
        jsonObject.addProperty("type", scriptActionFrame.getAction().getActionTypeId());
        jsonObject.add("data", jsonSerializationContext.serialize(scriptActionFrame.getAction()));
        return jsonObject;
    }
}

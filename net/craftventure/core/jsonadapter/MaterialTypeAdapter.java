package net.craftventure.core.jsonadapter;

import com.google.gson.*;
import org.bukkit.Material;

import java.lang.reflect.Type;


public class MaterialTypeAdapter implements JsonSerializer<Material>, JsonDeserializer<Material> {
    @Override
    public Material deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        Material material = Material.valueOf(jsonObject.get("type").getAsString());
        return material;
    }

    @Override
    public JsonElement serialize(Material material, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", material.name());
        jsonObject.addProperty("id", material.getId());
        return jsonObject;
    }
}

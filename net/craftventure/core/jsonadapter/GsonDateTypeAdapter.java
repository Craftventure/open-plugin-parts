package net.craftventure.core.jsonadapter;


import com.google.gson.*;
import net.craftventure.core.ktx.util.Logger;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class GsonDateTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {
    private final DateFormat dateFormat;
    private final DateFormat dateFormatWithoutTz;
    private final DateFormat dateFormatWithoutTzWithoutT;

    public GsonDateTypeAdapter() {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        dateFormatWithoutTz = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormatWithoutTzWithoutT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    @Override
    public synchronized JsonElement serialize(Date date, Type type, JsonSerializationContext jsonSerializationContext) {
        synchronized (dateFormat) {
            String dateFormatAsString = dateFormat.format(date);
            return new JsonPrimitive(dateFormatAsString);
        }
    }

    @Override
    public synchronized Date deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) {
        String s = jsonElement.getAsJsonPrimitive().getAsString();
        try {
            synchronized (dateFormat) {
                return dateFormat.parse(s);
            }
        } catch (Exception e2) {
            try {
                synchronized (dateFormatWithoutTz) {
                    return dateFormatWithoutTz.parse(s);
                }
            } catch (Exception e3) {
                try {
                    synchronized (dateFormatWithoutTzWithoutT) {
                        return dateFormatWithoutTzWithoutT.parse(s);
                    }
                } catch (Exception e4) {
                    Logger.capture(e4);
                }
            }
        }
        return null;
    }
}

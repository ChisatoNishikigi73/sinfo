package lycrex.sinfo.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonUtils {
    private static final Gson GSON = new Gson();

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final JsonObject jsonObject = new JsonObject();

        public Builder add(String key, String value) {
            jsonObject.addProperty(key, value);
            return this;
        }

        public Builder add(String key, Number value) {
            jsonObject.addProperty(key, value);
            return this;
        }

        public Builder add(String key, Boolean value) {
            jsonObject.addProperty(key, value);
            return this;
        }

        public Builder add(String key, JsonElement value) {
            jsonObject.add(key, value);
            return this;
        }

        public Builder add(String key, Builder builder) {
            jsonObject.add(key, builder.buildElement());
            return this;
        }

        public JsonElement buildElement() {
            return jsonObject;
        }

        public String build() {
            return GSON.toJson(jsonObject);
        }
    }

    public static JsonArray array() {
        return new JsonArray();
    }
}


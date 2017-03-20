package github.tylerjmcbride.direct.utilities;

import com.bluelinelabs.logansquare.typeconverters.TypeConverter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;

public final class ClassConverter implements TypeConverter<Class> {

    @Override
    public Class parse(JsonParser jsonParser) throws IOException {
        try {
            return Class.forName(jsonParser.getValueAsString());
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void serialize(Class object, String fieldName, boolean writeFieldNameForObject, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeFieldName(fieldName);
        jsonGenerator.writeString(object.getCanonicalName());
    }
}

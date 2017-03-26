package github.tylerjmcbride.direct.model.data;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import github.tylerjmcbride.direct.utilities.ClassConverter;

@JsonObject
public abstract class Data {
    @JsonField(typeConverter = ClassConverter.class)
    Class type;

    public Data() {
        this.type = this.getClass();
    }
}

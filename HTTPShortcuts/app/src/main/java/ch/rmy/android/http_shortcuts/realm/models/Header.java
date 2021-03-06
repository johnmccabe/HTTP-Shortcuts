package ch.rmy.android.http_shortcuts.realm.models;

import ch.rmy.android.http_shortcuts.key_value_pairs.KeyValuePair;
import io.realm.RealmObject;
import io.realm.annotations.Required;

public class Header extends RealmObject implements KeyValuePair {

    public static final String[] SUGGESTED_KEYS = {
            "Accept",
            "Accept-Charset",
            "Accept-Encoding",
            "Accept-Language",
            "Accept-Datetime",
            "Authorization",
            "Cache-Control",
            "Connection",
            "Cookie",
            "Content-Length",
            "Content-MD5",
            "Content-Type",
            "Date",
            "Expect",
            "Forwarded",
            "From",
            "Host",
            "If-Match",
            "If-Modified-Since",
            "If-None-Match",
            "If-Range",
            "If-Unmodified-Since",
            "Max-Forwards",
            "Origin",
            "Pragma",
            "Proxy-Authorization",
            "Range",
            "Referer",
            "TE",
            "User-Agent",
            "Upgrade",
            "Via",
            "Warning"
    };

    @Required
    private String key;
    @Required
    private String value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static Header createNew(String key, String value) {
        Header header = new Header();
        header.setKey(key);
        header.setValue(value);
        return header;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Header header = (Header) o;

        if (!getKey().equals(header.getKey())) return false;
        return getValue().equals(header.getValue());

    }

    @Override
    public int hashCode() {
        int result = getKey().hashCode();
        result = 31 * result + getValue().hashCode();
        return result;
    }

}

package dev.crater.utils.jar;

import lombok.Getter;
import lombok.Setter;

public class ResourceWrapper {
    @Getter
    private String originEntryName;
    @Getter
    @Setter
    private byte[] bytes;
    public ResourceWrapper(String originEntryName, byte[] bytes) {
        this.originEntryName = originEntryName;
        this.bytes = bytes;
    }
}

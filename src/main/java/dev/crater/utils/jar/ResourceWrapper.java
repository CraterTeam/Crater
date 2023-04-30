package dev.crater.utils.jar;

import lombok.Getter;
import lombok.Setter;

public class ResourceWrapper {
    @Getter
    private String originEntryName;
    @Getter
    @Setter
    private byte[] bytes;
    @Getter
    @Setter
    private String entryName;
    public ResourceWrapper(String originEntryName, byte[] bytes) {
        this.originEntryName = originEntryName;
        this.entryName = originEntryName;
        this.bytes = bytes;
    }
}

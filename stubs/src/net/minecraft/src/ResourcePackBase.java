package net.minecraft.src;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ResourcePackBase implements IResourcePack {
    public File file; // made public by __TexturePackBase

    public InputStream getInputStream(ResourceAddress var1) throws IOException {
        return null;
    }

    public boolean hasResource(ResourceAddress var1) {
        return false;
    }

    public List<String> getNamespaces() {
        return null;
    }

    public MCMetaResourcePackInfo getPackInfo(MCMetaParser var1) throws IOException {
        return null;
    }

    public BufferedImage getPackIcon() throws IOException {
        return null;
    }
}

package org.example;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Trail {
    int version;
    String name;
    String author;
    String description;
    long lastUpdated;
    String icon;
    HashMap<String, String> images; //key = image name, value = path in zip file
    HashMap<String, BufferedImage> loadedImages; //key = image name, value = loaded image
    Boolean keepDefaultTrail;
    long workshopId;

    public static final float SUPERSPEED_THRESHOLD = 800.0f;
    public static final float AFTERIMAGE_THRESHOLD = 400.0f;

    public static final int ENABLED_NEVER = 0;
    public static final int ENABLED_ALWAYS = 1;
    public static final int ENABLED_ONLY_SUPERSPEED = 2;
    public static final int ENABLED_NOT_SUPERSPEED = 3;

    List<TrailLayer> layers;
    float totalTime;

    Trail() {
        version = 0;
        name = "";
        author = "";
        description = "";
        lastUpdated = 0;
        icon = "icon";
        images = new HashMap<>();
        loadedImages = new HashMap<>();
        keepDefaultTrail = false;
        workshopId = 0;
        totalTime = 0.0f;

        layers = new ArrayList<TrailLayer>();
    }

    Trail(String filename) throws IOException {
        images = new HashMap<>();
        loadedImages = new HashMap<>();

        layers = new ArrayList<TrailLayer>();

        ReadFromFile(filename);
    }

    public static float CalculateSpeed(Vector2 velocity) {
        return Vector2.Length(velocity);
        // return Math.abs(velocity.x);
    }

    public void ReadFromFile(String filename) throws IOException {
        try (ZipFile zipFile = new ZipFile(filename)) {
            //open settings.trail
            ZipEntry settingsEntry = zipFile.getEntry("settings.trail");
            if (settingsEntry == null) {
                throw new IOException("settings.trail not found in " + filename);
            }
            ReadSettings(new DataInputStream(zipFile.getInputStream(settingsEntry)), zipFile);
        } catch (IOException e) {
            throw new IOException("Failed to read trail file: " + filename, e);
        }
    }

    public void AddPoint(Vector2 position, Vector2 velocity) {
        for (TrailLayer layer : layers) {
            if (layer.layerType == 0) {
                TrailStripe stripe = (TrailStripe) layer;
                stripe.AddPoint(position, velocity, totalTime);
            }
        }
    }

    public void Update(float deltaTime, Vector2 position, Vector2 velocity) {
        totalTime += deltaTime;
        for (TrailLayer layer : layers) {
            layer.Update(deltaTime, position, velocity);
        }
        AddPoint(position, velocity);
    }

    public void Reset() {
        totalTime = 0.0f;
        for (TrailLayer layer : layers) {
            layer.Reset();
        }
    }

    private void ReadSettings(DataInputStream in, ZipFile zipFile) throws IOException {
        version = ReadInt4(in);
        name = ReadString(in);
        author = ReadString(in);
        description = ReadString(in);
        lastUpdated = ReadLong8(in);
        icon = ReadString(in);
        int imageCount = ReadInt4(in);
        images = new HashMap<>();
        for (int i = 0; i < imageCount; i++) {
            String key = ReadString(in);
            String path = ReadString(in);
            images.put(key, path);
        }

        for (HashMap.Entry<String, String> entry : images.entrySet()) {
            String imageName = entry.getKey();
            String imagePath = entry.getValue();
            ZipEntry imageEntry = zipFile.getEntry(imagePath);
            if (imageEntry == null) {
                throw new IOException("Image not found in zip: " + imagePath);
            }
            try (DataInputStream imageIn = new DataInputStream(zipFile.getInputStream(imageEntry))) {
                BufferedImage image = javax.imageio.ImageIO.read(imageIn);
                loadedImages.put(imageName, image);
            } catch (IOException e) {
                throw new IOException("Failed to read image: " + imagePath, e);
            }
        }

        int layerCount = ReadInt4(in);
        for (int i = 0; i < layerCount; i++) {
            byte type = in.readByte(); //0 = stripe, 1 = particle, 2 = animation
            int propertyCount = ReadInt4(in);
            HashMap<String, String> properties = new HashMap<>();
            for (int j = 0; j < propertyCount; j++) {
                String key = ReadString(in);
                String value = ReadString(in);
                properties.put(key, value);
            }

            switch (type) {
                case 0 -> {
                    TrailStripe stripe = new TrailStripe();
                    stripe.LoadFromHashMap(properties);
                    layers.add(stripe);
                }
                case 1 -> {
                    TrailParticleEmitter emitter = new TrailParticleEmitter();
                    emitter.LoadFromHashMap(properties);
                    if (!loadedImages.containsKey(emitter.image)) {
                        continue;
                    }
                    emitter.imageWidth = loadedImages.get(emitter.image).getWidth();
                    emitter.imageHeight = loadedImages.get(emitter.image).getHeight();
                    layers.add(emitter);
                }
                case 2 -> {
                    TrailAnimation animation = new TrailAnimation();
                    animation.LoadFromHashMap(properties);
                    if (!loadedImages.containsKey(animation.image)) {
                        continue;
                    }
                    animation.imageWidth = loadedImages.get(animation.image).getWidth();
                    animation.imageHeight = loadedImages.get(animation.image).getHeight();
                    layers.add(animation);
                }
                default -> throw new IOException("Unknown layer type: " + type);
            }
        }

        SortByOrder();

        //from pop
        if (version >= 2) {
            keepDefaultTrail = in.readByte() == 0;
        }
        if (version >= 3) {
            workshopId = ReadLong8(in);
        }
    }

    private void SortByOrder() {
        layers.sort((a, b) -> Integer.compare(a.order, b.order));
    }

    private int ReadInt4(DataInputStream in) throws IOException {
        int b1 = in.readUnsignedByte();
        int b2 = in.readUnsignedByte();
        int b3 = in.readUnsignedByte();
        int b4 = in.readUnsignedByte();
        return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
    }

    private long ReadLong8(DataInputStream in) throws IOException {
        long b1 = in.readUnsignedByte();
        long b2 = in.readUnsignedByte();
        long b3 = in.readUnsignedByte();
        long b4 = in.readUnsignedByte();
        long b5 = in.readUnsignedByte();
        long b6 = in.readUnsignedByte();
        long b7 = in.readUnsignedByte();
        long b8 = in.readUnsignedByte();
        return (b8 << 56) | (b7 << 48) | (b6 << 40) | (b5 << 32) | (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
    }

    private String ReadString(DataInputStream in) throws IOException {
        byte length = in.readByte();
        if (length == 0) return "";
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, "UTF-8");
    }
}
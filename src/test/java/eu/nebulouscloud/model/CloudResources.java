package eu.nebulouscloud.model;

import java.util.HashMap;
import java.util.Map;

public class CloudResources {
    private String uuid;
    private String title;
    private String platform;
    private String enabled;
    private String regions;

    public CloudResources(String uuid, String title, String platform, String enabled, String regions) {
        this.uuid = uuid;
        this.title = title;
        this.platform = platform;
        this.enabled = enabled;
        this.regions = regions;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public String getRegions() {
        return regions;
    }

    public void setRegions(String regions) {
        this.regions = regions;
    }
    public Map<String, Object> toMap() {
        Map<String, Object> resourceMap = new HashMap<>();
        resourceMap.put("uuid", this.uuid);
        resourceMap.put("title", this.title);
        resourceMap.put("platform", this.platform);
        resourceMap.put("enabled", this.enabled);
        resourceMap.put("regions", this.regions);
        return resourceMap;
    }


}

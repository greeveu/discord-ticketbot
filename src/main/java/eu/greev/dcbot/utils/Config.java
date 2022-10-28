package eu.greev.dcbot.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.io.IOException;

@Slf4j
public class Config {
    @Getter @Setter private long serverId;
    @Getter @Setter private long staffId;
    @Getter @Setter private long supportCategory;
    @Getter @Setter private long baseChannel;
    @Getter @Setter private String serverLogo;
    @Getter @Setter private String serverName;
    @Getter @Setter private String color;
    @Getter @Setter private String token;

    public void dumpConfig(String path) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);
        try {
            yaml.dump(this, new FileWriter(path));
        } catch (IOException e) {
            log.error("Failed creating FileWriter", e);
        }
    }
}
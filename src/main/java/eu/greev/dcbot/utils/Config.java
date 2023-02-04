package eu.greev.dcbot.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Slf4j
@Getter
@Setter
public class Config {
    private long serverId;
    private long staffId;
    private long supportCategory;
    private long baseChannel;
    private String serverLogo;
    private String serverName;
    private String color;
    private String token;
    private List<Long> addToTicketThread;


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
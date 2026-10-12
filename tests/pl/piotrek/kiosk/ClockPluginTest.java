package pl.piotrek.kiosk;
import java.util.*;
import me.jxl.kiosk.plugins.PluginHost;
public final class ClockPluginTest {
    static final class Host implements PluginHost {
        final Set<String> subscriptions=new HashSet<>();
        Map<String,Object> data; int publications; boolean stopped; ClockPlugin plugin;
        public void subscribe(String event) { subscriptions.add(event); }
        public void unsubscribe(String event) { subscriptions.remove(event); }
        public void publishScreensaver(String key,String title,String html) {
            assert !stopped : "host called after stop";
            assert html.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<524288;
            assert html.contains("data:font/ttf;base64,");
            assert html.contains("data:image/svg+xml;base64,");
            assert !html.contains("%CLOCK_OPTIONS_JSON%");
            this.data=new LinkedHashMap<>(plugin.renderData());
            assert html.contains("\"temperatureText\":\""+data.get("temperatureText")+"\"");
            publications++;
        }
        public void status(String message,boolean error) { assert !stopped; }
        public void showWindow(String a,String b,String c) { throw new AssertionError("overlay used"); }
        public void hideWindow() { }
        public void log(String message) { }
    }
    static Map<String,Object> snapshot(String status,String state,Object... attrs) {
        Map<String,Object> data=new HashMap<>(),attributes=new HashMap<>();
        data.put("status",status); data.put("state",state);
        for(int i=0;i<attrs.length;i+=2) attributes.put((String)attrs[i],attrs[i+1]);
        data.put("attributes",attributes); return data;
    }
    public static void main(String[] args) throws Exception {
        assert "overcast".equals(ClockPlugin.iconFor("cloudy",true,81));
        assert "cloudy".equals(ClockPlugin.iconFor("cloudy",false,80));
        assert "partlycloudy-night2".equals(ClockPlugin.iconFor("partlycloudy",false,20));
        assert "night".equals(ClockPlugin.iconFor("sunny",false,0));
        assert "unknown".equals(ClockPlugin.iconFor("unavailable",true,0));
        Map<String,Object> settings=new HashMap<>();
        settings.put("temperatureEntity","sensor.temp"); settings.put("weatherEntity","weather.home");
        settings.put("sunEntity","sun.sun"); settings.put("decimals","1");
        Host host=new Host(); ClockPlugin plugin=new ClockPlugin();
        host.plugin=plugin;
        plugin.start(host,settings);
        assert host.subscriptions.size()==3;
        assert host.data.get("temperatureText").equals("— °C");
        plugin.onEvent("ks.ha.entity.sensor.temp",snapshot("available","-2.34","unit_of_measurement","°C"));
        plugin.onEvent("ks.ha.entity.weather.home",snapshot("available","partlycloudy","cloud_coverage",10));
        plugin.onEvent("ks.ha.entity.sun.sun",snapshot("available","below_horizon"));
        plugin.configure(settings);
        assert host.data.get("temperatureText").equals("-2.3\u00a0°C");
        assert host.data.get("icon").equals("partlycloudy-night2");
        assert host.data.get("statusText").equals("");
        int before=host.publications;
        plugin.onEvent("ks.ha.entity.weather.home",snapshot("available","partlycloudy","cloud_coverage",10,"lastUpdated","different"));
        plugin.configure(settings);
        assert before==host.publications : "metadata-only update republished";
        plugin.onEvent("ks.ha.entity.sensor.temp",snapshot("disconnected",null));
        plugin.configure(settings);
        assert host.data.get("temperatureText").equals("-2.3\u00a0°C");
        assert host.data.get("statusText").toString().contains("nieaktualna");
        settings.put("temperatureEntity","sensor.other"); plugin.configure(settings);
        assert !host.subscriptions.contains("ha.entity.sensor.temp");
        assert host.data.get("temperatureText").equals("— °C");
        plugin.onEvent("ks.ha.entity.sensor.temp",snapshot("available","99")); plugin.configure(settings);
        assert host.data.get("temperatureText").equals("— °C");
        plugin.onEvent("ks.ha.entity.sensor.other",snapshot("available","NaN")); plugin.configure(settings);
        assert host.data.get("temperatureText").equals("— °C");
        settings.put("temperatureEntity","weather.home"); plugin.configure(settings);
        plugin.onEvent("ks.ha.entity.weather.home",snapshot("available","rainy","temperature",12.7,"temperature_unit","°F"));
        plugin.configure(settings);
        assert host.data.get("temperatureText").equals("12.7\u00a0°F");
        assert ClockPlugin.indicator("lock.door","locked","")[1].equals("Zaryglowany");
        assert ClockPlugin.indicator("lock.door","unlocked","")[1].equals("Odryglowany");
        assert ClockPlugin.indicator("cover.gate","opening","")[2].equals("moving");
        assert ClockPlugin.indicator("binary_sensor.door","on","door")[1].equals("Otwarte");
        assert ClockPlugin.indicator("binary_sensor.motion","off","motion")[1].equals("Wyłączone");
        settings.put("infoEntity1","lock.front"); settings.put("infoEntity2","cover.gate");
        settings.put("infoEntity3","lock.front"); settings.put("infoEntity5","binary_sensor.door");
        settings.put("infoLayout","Wejście|150|80;Brama|120|100;Drugi podpis|100|100;;Drzwi|100|100");
        plugin.configure(settings);
        assert host.data.get("infoPosition").equals("bottom");
        settings.put("infoLayout","layout=left;Wejście|150|80;Brama|120|100;Drugi podpis|100|100;;Drzwi|100|100");
        plugin.configure(settings);
        assert host.data.get("infoPosition").equals("left");
        assert host.data.get("info1Name").equals("Wejście");
        assert host.data.get("info5Name").equals("Drzwi");
        assert !host.data.get("info4Visible").equals(true);
        assert host.data.get("info1State").equals("Brak danych");
        plugin.onEvent("ks.ha.entity.lock.front",snapshot("available","locked"));
        plugin.onEvent("ks.ha.entity.cover.gate",snapshot("available","open"));
        plugin.onEvent("ks.ha.entity.binary_sensor.door",snapshot("available","off","device_class","door"));
        plugin.configure(settings);
        assert host.data.get("info1Name").equals("Wejście");
        assert host.data.get("info1IconSize").equals(150.0);
        assert host.data.get("info1State").equals("Zaryglowany");
        assert host.data.get("info3State").equals("Zaryglowany");
        assert host.data.get("info2State").equals("Otwarta");
        assert host.data.get("info5State").equals("Zamknięte");
        assert MdiIcons.path("mdi:gate").length()>20;
        assert MdiIcons.path("mdi:not-a-real-icon").equals("");
        assert host.data.get("info1Path").equals(MdiIcons.path("lock"));
        plugin.onEvent("ks.ha.entity.binary_sensor.door",snapshot("available","on","device_class","door","icon","mdi:window-open"));
        plugin.configure(settings);
        assert host.data.get("info5Path").equals(MdiIcons.path("window-open"));
        settings.put("infoLayout","Wejście|150|80|mdi:shield-lock|#00FF00|#112233;Brama|120|100;Drugi podpis|100|100;;Drzwi|100|100");
        plugin.configure(settings);
        assert host.data.get("info1Path").equals(MdiIcons.path("shield-lock"));
        assert host.data.get("info1ActiveColor").equals("#00FF00");
        assert host.data.get("info1InactiveColor").equals("#112233");
        settings.put("infoLayout",";Brama|120|100|custom:gate-left|#FF4B4B|#A0A0A0");
        for(String state : Arrays.asList("open","closed","opening","closing","unavailable")) {
            plugin.onEvent("ks.ha.entity.cover.gate",snapshot("available",state));
            plugin.configure(settings);
            if("unavailable".equals(state)) {
                assert !host.data.containsKey("info2CustomSvg");
                assert host.data.get("info2Tone").equals("unknown");
            } else {
                assert host.data.get("info2CustomSvg").equals(ClockPlugin.gateSvg(!"closed".equals(state)));
                assert host.data.get("info2Tone").equals("closed".equals(state)?"quiet":("open".equals(state)?"alert":"moving"));
            }
        }
        settings.put("infoEntity2","binary_sensor.gate");
        for(String state : Arrays.asList("on","off")) {
            plugin.configure(settings);
            plugin.onEvent("ks.ha.entity.binary_sensor.gate",snapshot("available",state));
            plugin.configure(settings);
            assert host.data.get("info2CustomSvg").equals(ClockPlugin.gateSvg("on".equals(state)));
        }
        plugin.onEvent("ks.ha.entity.binary_sensor.gate",snapshot("disconnected",null));
        plugin.configure(settings);
        assert !host.data.containsKey("info2CustomSvg") : "stale gate displayed as confirmed";
        for(String kind : Arrays.asList("garage","window","lock")) {
            String id="lock".equals(kind)?"lock.custom":"binary_sensor.custom";
            settings.put("infoEntity2",id);
            settings.put("infoLayout",";Test|100|100|custom:"+kind);
            plugin.configure(settings);
            for(String state : ("lock".equals(kind)?Arrays.asList("locked","unlocked","locking","unlocking","jammed","unavailable"):Arrays.asList("on","off","open","closed","unavailable"))) {
                plugin.onEvent("ks.ha.entity."+id,snapshot("available",state));plugin.configure(settings);
                if("unavailable".equals(state)||"jammed".equals(state)) {
                    assert !host.data.containsKey("info2CustomSvg");
                    assert host.data.get("info2Tone").equals("jammed".equals(state)?"alert":"unknown");
                } else {
                    boolean closed=Arrays.asList("locked","off","closed").contains(state);
                    assert host.data.get("info2CustomSvg").equals(CustomIcons.svg("custom:"+kind,!closed));
                    assert !CustomIcons.svg("custom:"+kind,true).equals(CustomIcons.svg("custom:"+kind,false));
                    assert host.data.get("info2InactiveColor").equals("#444444");
                }
            }
        }
        plugin.onEvent("ks.ha.entity.lock.front",snapshot("disconnected",null));
        String savedLayout=(String)settings.get("infoLayout");
        settings.put("infoLayout","icons=off;layout=left;"+savedLayout);
        plugin.configure(settings);
        for(int i=1;i<=5;i++) assert host.data.get("info"+i+"Visible").equals(false);
        assert host.subscriptions.contains("ha.entity."+settings.get("infoEntity2"));
        settings.put("infoLayout",savedLayout);plugin.configure(settings);
        assert host.data.get("info2Visible").equals(true);
        plugin.configure(settings);
        assert host.data.get("info1State").equals("Brak danych") : "stale lock displayed as confirmed";
        settings.put("infoEntity1",""); plugin.configure(settings);
        assert host.data.get("info1Visible").equals(false);
        plugin.onEvent("ks.ha.entity.weather.home",snapshot("available","sunny","temperature",15));
        settings.put("infoEntity1","binary_sensor.motion");settings.put("infoEntity2","cover.gate");
        settings.put("infoEntity3","");settings.put("infoEntity5","");
        settings.put("infoLayout","icons=active;layout=left;Ruch|100|100;Brama|100|100|custom:gate-left");
        plugin.configure(settings);
        plugin.onEvent("ks.ha.entity.binary_sensor.motion",snapshot("available","off"));
        plugin.onEvent("ks.ha.entity.cover.gate",snapshot("available","closed"));plugin.configure(settings);
        for(int i=1;i<=5;i++) assert host.data.get("info"+i+"Visible").equals(false);
        plugin.onEvent("ks.ha.entity.cover.gate",snapshot("available","open"));plugin.configure(settings);
        assert host.data.get("info2Visible").equals(true);
        assert host.data.get("info1Visible").equals(true) : "inactive slot missing from active strip";
        plugin.onEvent("ks.ha.entity.binary_sensor.motion",snapshot("available","on"));plugin.configure(settings);
        assert host.data.get("info1Visible").equals(true);
        plugin.onEvent("ks.ha.entity.cover.gate",snapshot("available","closed"));plugin.configure(settings);
        assert host.data.get("info2Visible").equals(true) : "inactive gate missing while motion active";
        plugin.onEvent("ks.ha.entity.cover.gate",snapshot("available","closing"));plugin.configure(settings);
        assert host.data.get("info2Visible").equals(true);
        plugin.onEvent("ks.ha.entity.cover.gate",snapshot("available","closed"));
        plugin.onEvent("ks.ha.entity.binary_sensor.motion",snapshot("available","off"));plugin.configure(settings);
        for(int i=1;i<=5;i++) assert host.data.get("info"+i+"Visible").equals(false);
        plugin.stop(); host.stopped=true;
        Thread.sleep(1100);
        plugin.onEvent("ks.ha.entity.weather.home",snapshot("available","rainy"));
        System.out.println("PASS: mapping, subscriptions, formatting, stale data, deduplication, replacement, shutdown");
    }
}

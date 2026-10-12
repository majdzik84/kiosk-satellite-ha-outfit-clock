// SPDX-License-Identifier: Apache-2.0
package pl.piotrek.kiosk;

import me.jxl.kiosk.plugins.KioskPlugin;
import me.jxl.kiosk.plugins.PluginHost;
import java.util.*;
import java.util.concurrent.*;

/** HA reads stay in the Java plugin. The renderer gets only ready-to-display scalars. */
public final class ClockPlugin implements KioskPlugin {
    private PluginHost host;
    private Map<String,Object> settings = new LinkedHashMap<>();
    private final Set<String> subscriptions = new HashSet<>();
    private final Map<String,Map<String,Object>> snapshots = new HashMap<>();
    private final Map<String,Map<String,Object>> lastGood = new HashMap<>();
    private Map<String,Object> published;
    private ScheduledExecutorService worker;
    private ScheduledFuture<?> pending;
    private long lastPublish;

    @Override public synchronized void start(PluginHost host, Map<String,Object> settings) {
        this.host = host;
        worker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r,"ha-clock-publisher"); t.setDaemon(true); return t;
        });
        configure(settings);
    }

    @Override public synchronized void configure(Map<String,Object> next) {
        if (host == null) return;
        settings = new LinkedHashMap<>(next);
        Set<String> desired = new HashSet<>();
        for (String key : Arrays.asList("temperatureEntity","weatherEntity","sunEntity")) {
            String id = text(settings.get(key));
            if (!id.isEmpty()) desired.add(id);
        }
        for (int i=1;i<=5;i++) {
            String id=text(settings.get("infoEntity"+i));
            if (!id.isEmpty()) desired.add(id);
        }
        for (String id : new HashSet<>(subscriptions)) if (!desired.contains(id)) {
            host.unsubscribe("ha.entity." + id);
            subscriptions.remove(id); snapshots.remove(id); lastGood.remove(id);
        }
        for (String id : desired) if (subscriptions.add(id)) host.subscribe("ha.entity." + id);
        if (pending != null) { pending.cancel(false); pending = null; }
        publish();
    }

    @Override public synchronized void onEvent(String event, Map<String,Object> payload) {
        String prefix = "ks.ha.entity.";
        if (host == null || !event.startsWith(prefix)) return;
        String id = event.substring(prefix.length());
        if (!subscriptions.contains(id)) return;
        Map<String,Object> copy = new LinkedHashMap<>(payload);
        snapshots.put(id,copy);
        if ("available".equals(copy.get("status"))) lastGood.put(id,copy);
        // Ignore metadata-only samples. Recreating the WebView is expensive.
        if (Objects.equals(renderData(),published)) return;
        if (pending == null || pending.isDone()) {
            long since = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastPublish);
            long delay = Math.max(1000,15000-since);
            pending = worker.schedule(() -> {
                synchronized (ClockPlugin.this) {
                    pending = null;
                    if (host == null) return;
                    try { publish(); }
                    catch (RuntimeException error) {
                        if (host != null) {
                            try { host.status("Nie udało się odświeżyć wygaszacza: " + error.getClass().getSimpleName(),true); }
                            catch (RuntimeException ignored) { }
                        }
                    }
                }
            },delay,TimeUnit.MILLISECONDS);
        }
    }

    private void publish() {
        Map<String,Object> data = renderData();
        if (data.equals(published)) return;
        host.publishScreensaver("clock","Zegar i pogoda",InlineRenderer.html(json(data)));
        published = data;
        lastPublish = System.nanoTime();
        host.status(text(data.get("statusText")).isEmpty() ? "Dane HA aktualne. Strefa czasu: Europe/Warsaw." : text(data.get("statusText")),false);
    }

    private Map<String,Object> reading(String key) {
        String id = text(settings.get(key));
        return lastGood.containsKey(id) ? lastGood.get(id) : Collections.emptyMap();
    }
    private boolean fresh(String key) {
        Map<String,Object> snapshot = snapshots.get(text(settings.get(key)));
        return snapshot != null && "available".equals(snapshot.get("status"));
    }
    private static Map<?,?> attributes(Map<String,Object> snapshot) {
        Object attrs = snapshot.get("attributes");
        return attrs instanceof Map ? (Map<?,?>)attrs : Collections.emptyMap();
    }
    private static String text(Object value) { return value instanceof String ? (String)value : ""; }
    private static double numeric(Object value) {
        try { return Double.parseDouble(String.valueOf(value)); }
        catch (NumberFormatException error) { return Double.NaN; }
    }

    Map<String,Object> renderData() {
        Map<String,Object> data = new LinkedHashMap<>();
        for (String key : Arrays.asList("clockSize","dateSize","temperatureSize","iconSize","clockColor","dateColor","temperatureColor","oneColor","backgroundColor","highlightOnes")) {
            if (settings.containsKey(key)) data.put(key,settings.get(key));
        }
        Map<String,Object> temp = reading("temperatureEntity");
        boolean weatherTemp = text(settings.get("temperatureEntity")).startsWith("weather.");
        double value = numeric(weatherTemp ? attributes(temp).get("temperature") : temp.get("state"));
        String unit = text(attributes(temp).get(weatherTemp ? "temperature_unit" : "unit_of_measurement"));
        if (unit.isEmpty()) unit = "°C";
        // Keep display payload bounded even for a misconfigured entity.
        if (unit.length() > 12) unit = unit.substring(0,12);
        int decimals = "0".equals(settings.get("decimals")) ? 0 : 1;
        data.put("temperatureText",Double.isFinite(value) ? String.format(Locale.US,"%."+decimals+"f",value)+"\u00a0"+unit : "— " + unit);
        Map<String,Object> weather = reading("weatherEntity");
        String condition = text(weather.get("state"));
        String sun = text(reading("sunEntity").get("state"));
        boolean day;
        if ("above_horizon".equals(sun)) day = true;
        else if ("below_horizon".equals(sun)) day = false;
        else day = !"clear-night".equals(condition);
        String icon = iconFor(condition,day,numeric(attributes(weather).get("cloud_coverage")));
        data.put("icon",icon);
        data.put("conditionLabel",labelFor(condition));
        List<String> issues = new ArrayList<>();
        if (!Double.isFinite(value)) issues.add("Brak temperatury");
        else if (!fresh("temperatureEntity")) issues.add("Temperatura nieaktualna");
        if ("unknown".equals(icon)) issues.add("Brak danych pogodowych");
        else if (!fresh("weatherEntity")) issues.add("Pogoda nieaktualna");
        if (!text(settings.get("sunEntity")).isEmpty() && !fresh("sunEntity")) issues.add("Brak aktualnego stanu słońca");
        StringBuilder status = new StringBuilder();
        for (String issue : issues) {
            if (status.length() > 0) status.append(" · ");
            status.append(issue);
        }
        data.put("statusText",status.toString());
        String layout=text(settings.get("infoLayout"));
        boolean iconsEnabled=!layout.startsWith("icons=off;");
        boolean activeOnly=layout.startsWith("icons=active;");
        if (!iconsEnabled) layout=layout.substring("icons=off;".length());
        else if (activeOnly) layout=layout.substring("icons=active;".length());
        String position="bottom";
        if (layout.startsWith("layout=left;") || layout.startsWith("layout=bottom;")) {
            int separator=layout.indexOf(';');
            position=layout.substring(7,separator);
            layout=layout.substring(separator+1);
        }
        data.put("infoPosition",position);
        String[] rows=layout.split(";",-1);
        for (int i=1;i<=5;i++) {
            String id=text(settings.get("infoEntity"+i));
            data.put("info"+i+"Visible",iconsEnabled && !id.isEmpty());
            if (!iconsEnabled || id.isEmpty()) continue;
            String[] row=(rows.length>=i ? rows[i-1] : "").split("\\|",-1);
            Map<String,Object> snapshot=snapshots.get(id);
            if (snapshot==null) snapshot=Collections.emptyMap();
            String name=row[0].trim();
            if (name.isEmpty()) name=text(attributes(snapshot).get("friendly_name"));
            if (name.isEmpty()) name="Pozycja "+i;
            data.put("info"+i+"Name",name.substring(0,Math.min(60,name.length())));
            data.put("info"+i+"IconSize",scale(row,1));
            data.put("info"+i+"TextSize",scale(row,2));
            String state=fresh("infoEntity"+i) ? text(snapshot.get("state")) : "unavailable";
            String[] display=indicator(id,state,text(attributes(snapshot).get("device_class")));
            data.put("info"+i+"Icon",display[0]);
            data.put("info"+i+"State",display[1]);
            data.put("info"+i+"Tone",display[2]);
            String override=row.length>3 ? row[3].trim() : "";
            if (CustomIcons.has(override)) {
                boolean lock="custom:lock".equals(override);
                boolean closed="closed".equals(state) || "off".equals(state) || (lock && "locked".equals(state));
                boolean open="open".equals(state) || "on".equals(state) || (lock && "unlocked".equals(state));
                boolean moving="opening".equals(state) || "closing".equals(state) || (lock && ("locking".equals(state) || "unlocking".equals(state)));
                if (closed || open || moving) {
                    data.put("info"+i+"CustomSvg",CustomIcons.svg(override,!closed));
                    display=new String[]{"gate",closed ? "Zamknięta" : (moving ? ("opening".equals(state) ? "Otwieranie" : "Zamykanie") : "Otwarta"),closed ? "quiet" : (moving ? "moving" : "alert")};
                    if(lock) display[1]=closed ? "Zaryglowany" : (moving ? ("locking".equals(state)?"Ryglowanie":"Odryglowywanie") : "Odryglowany");
                    data.put("info"+i+"State",display[1]);
                    data.put("info"+i+"Tone",display[2]);
                } else if (!(lock && "jammed".equals(state))) {
                    display=new String[]{"unknown","Brak danych","unknown"};
                    data.put("info"+i+"State",display[1]);
                    data.put("info"+i+"Tone",display[2]);
                }
            }
            String haIcon=text(attributes(snapshot).get("icon"));
            String chosen=!override.isEmpty() ? override : haIcon;
            String path=MdiIcons.path(chosen);
            if(path.isEmpty()) path=MdiIcons.path(MdiIcons.automatic(id,state,text(attributes(snapshot).get("device_class"))));
            if("unknown".equals(display[2])) path=MdiIcons.path("help-circle-outline");
            data.put("info"+i+"Path",path);
            data.put("info"+i+"ActiveColor",rowColor(row,4,"#FF4B4B"));
            data.put("info"+i+"InactiveColor",rowColor(row,5,"#444444"));
        }
        if(activeOnly) {
            boolean anyActive=false;
            for(int i=1;i<=5;i++) if(Arrays.asList("alert","active","moving").contains(data.get("info"+i+"Tone"))) anyActive=true;
            for(int i=1;i<=5;i++) data.put("info"+i+"Visible",anyActive && !text(settings.get("infoEntity"+i)).isEmpty());
        }
        return data;
    }
    private static double scale(String[] row,int index) {
        double value=row.length>index ? numeric(row[index].trim()) : 100;
        return Double.isFinite(value) ? Math.max(50,Math.min(200,value)) : 100;
    }
    static String gateSvg(boolean open) {
        return CustomIcons.svg("custom:gate-left",open);
    }
    private static String rowColor(String[] row,int index,String fallback) {
        String value=row.length>index ? row[index].trim() : "";
        return value.matches("#[0-9a-fA-F]{6}") ? value : fallback;
    }
    static String[] indicator(String id,String state,String deviceClass) {
        if (state.isEmpty() || "unknown".equals(state) || "unavailable".equals(state))
            return new String[]{"unknown","Brak danych","unknown"};
        if (id.startsWith("lock.")) {
            switch(state) {
                case "locked": return new String[]{"lock","Zaryglowany","quiet"};
                case "unlocked": return new String[]{"unlock","Odryglowany","alert"};
                case "open": return new String[]{"unlock","Otwarty","alert"};
                case "locking": return new String[]{"lock","Ryglowanie","moving"};
                case "unlocking": return new String[]{"unlock","Odryglowywanie","moving"};
                case "opening": return new String[]{"unlock","Otwieranie","moving"};
                case "jammed": return new String[]{"warning","Zablokowany","alert"};
            }
        }
        if (id.startsWith("cover.")) {
            switch(state) {
                case "closed": return new String[]{"gate","Zamknięta","quiet"};
                case "open": return new String[]{"gate-open","Otwarta","alert"};
                case "opening": return new String[]{"gate-open","Otwieranie","moving"};
                case "closing": return new String[]{"gate","Zamykanie","moving"};
            }
        }
        if (id.startsWith("binary_sensor.") && Arrays.asList("door","window","opening","garage_door").contains(deviceClass)) {
            if ("on".equals(state)) return new String[]{"door-open","Otwarte","alert"};
            if ("off".equals(state)) return new String[]{"door","Zamknięte","quiet"};
        }
        if ("on".equals(state)) return new String[]{"indicator","Włączone","active"};
        if ("off".equals(state)) return new String[]{"indicator","Wyłączone","quiet"};
        return new String[]{"indicator",state.substring(0,Math.min(40,state.length())),"unknown"};
    }

    static String iconFor(String condition, boolean day, double coverage) {
        if ("cloudy".equals(condition) && coverage > 80) return "overcast";
        switch(condition) {
            case "clear": case "sunny": return day ? "day" : "night";
            case "clear-night": return "night";
            case "partlycloudy": return day ? "partlycloudy" : "partlycloudy-night2";
            case "pouring": return "rainy";
            case "exceptional": return "lightning";
            case "cloudy": case "fog": case "hail": case "lightning": case "lightning-rainy":
            case "overcast": case "rainy": case "snowy": case "snowy-rainy": case "windy": case "windy-variant": return condition;
            default: return "unknown";
        }
    }
    private static String json(Map<String,Object> data) {
        StringBuilder result=new StringBuilder("{");
        for (Map.Entry<String,Object> entry : data.entrySet()) {
            if (result.length()>1) result.append(',');
            result.append(quote(entry.getKey())).append(':');
            Object value=entry.getValue();
            if (value instanceof Boolean || value instanceof Number) result.append(value);
            else if (value==null) result.append("null");
            else result.append(quote(String.valueOf(value)));
        }
        return result.append('}').toString();
    }
    private static String quote(String value) {
        StringBuilder result=new StringBuilder("\"");
        for (int i=0;i<value.length();i++) {
            char c=value.charAt(i);
            if (c=='"' || c=='\\') result.append('\\').append(c);
            else if (c<32 || c=='<' || c=='>' || c=='&' || c=='\u2028' || c=='\u2029')
                result.append(String.format(Locale.US,"\\u%04x",(int)c));
            else result.append(c);
        }
        return result.append('"').toString();
    }
    private static String labelFor(String condition) {
        switch(condition) {
            case "clear": case "sunny": return "Słonecznie";
            case "clear-night": return "Bezchmurna noc";
            case "cloudy": case "overcast": return "Pochmurno";
            case "partlycloudy": return "Częściowe zachmurzenie";
            case "fog": return "Mgła";
            case "hail": return "Grad";
            case "lightning": case "exceptional": return "Burza";
            case "lightning-rainy": return "Burza z deszczem";
            case "pouring": case "rainy": return "Deszcz";
            case "snowy": return "Śnieg";
            case "snowy-rainy": return "Deszcz ze śniegiem";
            case "windy": case "windy-variant": return "Wietrznie";
            default: return "Brak danych pogodowych";
        }
    }
    @Override public void execute(String command, Map<String,Object> arguments) { }
    @Override public synchronized void stop() {
        host = null;
        if (pending != null) pending.cancel(true);
        if (worker != null) worker.shutdownNow();
        worker = null; pending = null; published = null;
        subscriptions.clear(); snapshots.clear(); lastGood.clear();
    }
}

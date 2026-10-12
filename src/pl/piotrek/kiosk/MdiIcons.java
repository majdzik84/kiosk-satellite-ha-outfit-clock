package pl.piotrek.kiosk;
import java.util.*;
import java.io.*;
import java.util.zip.GZIPInputStream;
/** Offline MDI lookup; no external image requests or HA frontend dependencies. */
final class MdiIcons {
    private static final class Holder {
        static final Map<String,String> PATHS=load();
    }
    static String path(String name) {
        if (name.startsWith("mdi:")) name=name.substring(4);
        String value=Holder.PATHS.get(name);
        return value==null ? "" : value;
    }
    private static Map<String,String> load() {
        try {
            String base64=MdiData.encoded(),alphabet="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
            ByteArrayOutputStream decoded=new ByteArrayOutputStream();
            int bits=0,value=0;
            for(int i=0;i<base64.length();i++) {
                int code=alphabet.indexOf(base64.charAt(i));
                if(code<0) continue;
                value=(value<<6)|code;bits+=6;
                if(bits>=8) {bits-=8;decoded.write((value>>bits)&255);}
            }
            Map<String,String> paths=new HashMap<>();
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(new GZIPInputStream(new ByteArrayInputStream(decoded.toByteArray())),"UTF-8"))) {
                String line;
                while((line=reader.readLine())!=null) {int split=line.indexOf('\t');if(split>0) paths.put(line.substring(0,split),line.substring(split+1));}
            }
            return paths;
        } catch(IOException error) {throw new IllegalStateException("Local MDI catalog unreadable",error);}
    }
    static String automatic(String id,String state,String deviceClass) {
        boolean on="on".equals(state)||"open".equals(state)||"unlocked".equals(state);
        if(id.startsWith("lock.")) return on ? "lock-open-variant" : "lock";
        if(id.startsWith("cover.")) return "garage_door".equals(deviceClass) ? (on ? "garage-open" : "garage") : (on ? "gate-open" : "gate");
        switch(deviceClass) {
            case "door":case "opening":return on ? "door-open" : "door-closed";
            case "window":return on ? "window-open-variant" : "window-closed-variant";
            case "garage_door":return on ? "garage-open" : "garage";
            case "motion":return on ? "motion-sensor" : "motion-sensor-off";
            case "occupancy":case "presence":return "home-account";
            case "moisture":return "water-alert";
            case "smoke":return "smoke-detector";
            case "gas":case "carbon_monoxide":return "gas-cylinder";
            case "battery":return "battery";
            case "connectivity":return "wifi";
            case "safety":case "problem":return "alert-circle-outline";
            case "lock":return on ? "lock-open" : "lock";
            case "plug":return "power-plug";
            case "light":return "lightbulb";
            case "temperature":return "thermometer";
            case "vibration":return "vibrate";
            case "sound":return "volume-high";
            case "heat":return "fire";
            case "cold":return "snowflake";
            case "tamper":return "shield-alert";
            case "update":return "update";
            case "power":return "flash";
            default:return "checkbox-blank-circle-outline";
        }
    }
}

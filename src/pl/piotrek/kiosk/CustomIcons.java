// Generated from resources/custom-icons.json.
package pl.piotrek.kiosk;
final class CustomIcons {
 static boolean has(String key) { return "custom:gate-left".equals(key) || "custom:garage".equals(key) || "custom:window".equals(key) || "custom:lock".equals(key); }
 static String svg(String key, boolean open) {
  if ("custom:gate-left".equals(key)) return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 64 64\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"butt\" stroke-linejoin=\"miter\"><path stroke-width=\"1.15\" d=\"%PATH%\"/><path stroke-width=\"2.76\" d=\"M57 18V50\"/></svg>".replace("%PATH%",open ? "M2 18H25V50H2ZM3 26H24M3 34H24M3 42H24" : "M7 18H57V50H7ZM8 26H56M8 34H56M8 42H56");
  if ("custom:garage".equals(key)) return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 64 64\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"butt\" stroke-linejoin=\"miter\"><path stroke-width=\"1.15\" d=\"%PATH%\"/></svg>".replace("%PATH%",open ? "M10 55V17H54V55M16 55V24H48V55M7 55H57M17 28H47M17 32H47" : "M10 55V17H54V55M16 55V24H48V55M7 55H57M17 32H47M17 40H47M17 48H47");
  if ("custom:window".equals(key)) return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 64 64\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"butt\" stroke-linejoin=\"miter\"><path stroke-width=\"1.15\" d=\"%PATH%\"/></svg>".replace("%PATH%",open ? "M15 10H49V55H15ZM20 15H44V50H20ZM32 16V49M32 15L54 21V57L32 50ZM48 35V39" : "M15 10H49V55H15ZM20 15H44V50H20ZM32 16V49M37 32H40");
  if ("custom:lock".equals(key)) return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 64 64\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"butt\" stroke-linejoin=\"miter\"><path stroke-width=\"1.15\" d=\"%PATH%\"/></svg>".replace("%PATH%",open ? "M21 28V19A11 11 0 0 1 43 19V23M17 29H47V54H17ZM34 39A2 2 0 1 1 30 39A2 2 0 1 1 34 39M32 41V46" : "M21 28V19A11 11 0 0 1 43 19V28M17 29H47V54H17ZM34 39A2 2 0 1 1 30 39A2 2 0 1 1 34 39M32 41V46");
  return "";
 }
}

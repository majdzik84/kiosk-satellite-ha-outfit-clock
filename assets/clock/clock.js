'use strict';
let options = window.clockOptions || {};
if (!window.clockOptions) {
 try { options = JSON.parse(decodeURIComponent(location.hash.slice(1) || '%7B%7D')); } catch (_) {}
}
const $ = id => document.getElementById(id);
const number = (key, fallback) => Number.isFinite(Number(options[key])) && options[key] != null ? Number(options[key]) : fallback;
const color = (key, fallback) => /^#[0-9a-f]{6}$/i.test(options[key] || '') ? options[key] : fallback;
document.body.style.backgroundColor = color('backgroundColor', '#000000');
document.documentElement.style.backgroundColor = color('backgroundColor', '#000000');
$('clock').style.color = color('clockColor', '#FFFFFF');
$('tempValue').style.color = color('temperatureColor', '#FFFFFF');
document.documentElement.style.setProperty('--one', color('oneColor', '#FF0000'));
document.documentElement.style.setProperty('--date', color('dateColor', '#A0A0A0'));
const icons = new Set(['day','night','cloudy','overcast','fog','hail','lightning','lightning-rainy','partlycloudy','partlycloudy-night2','rainy','snowy','snowy-rainy','windy','windy-variant','unknown']);
const icon = icons.has(options.icon) ? options.icon : 'unknown';
$('weatherIcon').src = 'weather-icons/' + icon + '.svg';
$('weatherIcon').alt = options.conditionLabel || 'Brak danych pogodowych';
$('tempValue').textContent = options.temperatureText || '— °C';
$('status').textContent = options.statusText || '';
const indicatorPaths = {
 lock:'M7 11V7a5 5 0 0 1 10 0v4M5 11h14v11H5zM12 15v3',
 unlock:'M7 11V7a5 5 0 0 1 9-3M5 11h14v11H5zM12 15v3',
 gate:'M3 22V3h18v19M3 7h18M7 7v15M12 7v15M17 7v15',
 'gate-open':'M3 22V3h18v19M3 7h18M3 12h18M3 16h18',
 door:'M5 22V2h14v20M2 22h20M15 12h1',
 'door-open':'M5 22V2h14v20M5 2l9 3v17l-9-3M10 12h1M2 22h20',
 warning:'M12 2L1 22h22zM12 9v6M12 18v1',
 unknown:'M9 7a3 3 0 1 1 5 2c-2 1-2 2-2 4M12 17v1',
 indicator:'M12 3v9M6 5a9 9 0 1 0 12 0'
};
for (let i=1;i<=5;i++) {
 if (!options['info'+i+'Visible']) continue;
 const item=document.createElement('div');
 item.infoIndex=i;
 const tone=options['info'+i+'Tone'];
 item.className='info-item '+(['quiet','alert','moving','active','unknown'].includes(tone) ? tone : 'unknown');
 const type=options['info'+i+'Icon'];
 const mdi=options['info'+i+'Path'];
 const path=typeof mdi==='string' && /^[Mm][0-9a-zA-Z., +\-]+$/.test(mdi) ? mdi : (indicatorPaths[type] || indicatorPaths.unknown);
 const img=document.createElement('img');
 img.className='info-icon';
 const ink=tone==='quiet' ? color('info'+i+'InactiveColor','#444444') : (tone==='unknown' ? '#777777' : color('info'+i+'ActiveColor','#FF4B4B'));
 const custom=options['info'+i+'CustomSvg'];
 const svg=typeof custom==='string' && custom.startsWith('<svg ') ? custom.replace(/currentColor/g,ink) : '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path d="'+path+'" fill="'+(mdi ? ink : 'none')+'" stroke="'+(mdi ? 'none' : ink)+'" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg>';
 img.src='data:image/svg+xml;base64,'+btoa(svg);
 img.alt=options['info'+i+'State'] || 'Brak danych';
 img.style.width=img.style.height=Math.max(24,innerWidth*.05*number('info'+i+'IconSize',100)/100)+'px';
 const name=document.createElement('div'); name.className='info-name'; name.textContent=options['info'+i+'Name']||'';
 item.title=img.alt;
 const textScale=number('info'+i+'TextSize',100)/100;
 name.style.fontSize=Math.max(12,innerWidth*.02*textScale)+'px';
 item.appendChild(img); item.appendChild(name); $('indicators').appendChild(item);
}
const days = ['Niedziela','Poniedziałek','Wtorek','Środa','Czwartek','Piątek','Sobota'];
const clockFormatter = new Intl.DateTimeFormat('pl-PL',{timeZone:'Europe/Warsaw',hour:'2-digit',minute:'2-digit',hourCycle:'h23'});
const dateFormatter = new Intl.DateTimeFormat('en-GB',{timeZone:'Europe/Warsaw',year:'numeric',month:'2-digit',day:'2-digit',weekday:'short'});
const weekdayMap = {Sun:0,Mon:1,Tue:2,Wed:3,Thu:4,Fri:5,Sat:6};
let previousMinute = '';
let previousDate = '';
function partsObject(parts) {
 const result = {};
 parts.forEach(part => { result[part.type] = part.value; });
 return result;
}
function updateTime() {
 const now = new Date();
 const parts = partsObject(clockFormatter.formatToParts(now));
 const time = parts.hour + ':' + parts.minute;
 if (time !== previousMinute) {
  previousMinute = time;
  $('clock').textContent = '';
  for (const digit of parts.hour) {
   const span = document.createElement('span');
   span.textContent = digit;
   if (digit === '1' && options.highlightOnes !== false) span.className = 'one';
   $('clock').appendChild(span);
  }
  $('clock').appendChild(document.createTextNode(':' + parts.minute));
 }
 const date = partsObject(dateFormatter.formatToParts(now));
 const dateText = days[weekdayMap[date.weekday]] + ', ' + date.day + '.' + date.month + '.' + date.year;
 if (dateText !== previousDate) {
  previousDate = dateText;
  $('date').textContent = dateText;
  layout();
 }
}
function layout() {
 const dashboard = $('dashboard');
 const left = options.infoPosition === 'left' && $('indicators').children.length > 0;
 dashboard.className = 'dashboard' + (left ? ' left' : '');
 dashboard.style.transform = 'none';
 dashboard.style.width = Math.min(1200,innerWidth-24)+'px';
 // The rail takes only its content width; long names wrap instead of reserving a wide strip.
 Array.from($('indicators').children).forEach(item => {
  const i=item.infoIndex;
  item.children[0].style.width=item.children[0].style.height=Math.max(24,innerWidth*.05*number('info'+i+'IconSize',100)/100)+'px';
  item.children[1].style.fontSize=Math.max(12,innerWidth*.02*number('info'+i+'TextSize',100)/100)+'px';
 });
 const width = left ? Math.max(100, dashboard.offsetWidth - $('indicators').offsetWidth - innerWidth*.02) : innerWidth;
 $('clock').style.fontSize = width * (width <= 600 ? .30 : .27) * number('clockSize',100) / 100 + 'px';
 $('date').style.fontSize = width * .05 * number('dateSize',100) / 100 + 'px';
 $('tempValue').style.fontSize = width * (width <= 600 ? .15 : .13) * number('temperatureSize',100) / 100 + 'px';
 const iconSize = width * .18 * number('iconSize',100) / 100;
 $('weatherIcon').style.width = $('weatherIcon').style.height = iconSize + 'px';
 const scale = Math.min(1, (innerWidth - 24) / Math.max(dashboard.scrollWidth,dashboard.offsetWidth), (innerHeight - 24) / dashboard.offsetHeight);
 dashboard.style.transform = 'scale(' + Math.max(.05,scale) + ')';
}
updateTime();
layout();
document.fonts.ready.then(layout);
$('weatherIcon').addEventListener('load',layout);
window.addEventListener('resize',layout);
document.addEventListener('visibilitychange',() => { if (!document.hidden) { updateTime(); layout(); } });
setInterval(updateTime,1000);

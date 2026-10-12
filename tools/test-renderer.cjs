// Execute the actual renderer against a small DOM fixture and a controllable clock.
const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname,'../assets/clock/clock.js'),'utf8');
function fixture(timestamp,options={},inline=false) {
 let now=timestamp;
 class Clock extends Date { constructor(...args) { super(...(args.length ? args : [now])); } }
 function element() {
  let text='';
  return {style:{setProperty(){}},children:[],offsetWidth:800,scrollWidth:800,offsetHeight:500,
   addEventListener(){},appendChild(child){this.children.push(child);},
   get textContent(){return text+this.children.map(c=>c.textContent).join('');},
   set textContent(value){text=value;this.children=[];}};
 }
 const elements=Object.fromEntries(['dashboard','clock','date','weatherIcon','tempValue','status','indicators'].map(id=>[id,element()]));
 const document={getElementById:id=>elements[id],body:element(),documentElement:element(),
  createElement:element,createTextNode:text=>({textContent:text}),
  fonts:{ready:Promise.resolve()},addEventListener(){}};
 const context=vm.createContext({document,Date:Clock,Intl,console,btoa:s=>Buffer.from(s,'binary').toString('base64'),location:{hash:'#'+encodeURIComponent(JSON.stringify(options))},
  innerWidth:800,innerHeight:1280,window:{addEventListener(){},...(inline ? {clockOptions:options} : {})},setInterval(){}});
 vm.runInContext(source,context);
 return {elements,update(timestamp){now=timestamp;vm.runInContext('updateTime()',context);}};
}
const clock=fixture('2026-10-04T09:15:00Z',{temperatureText:'18.4 °C',icon:'partlycloudy',oneColor:'#00FF00'});
assert.equal(clock.elements.clock.textContent,'11:15');
assert.equal(clock.elements.clock.children.filter(c=>c.className==='one').length,2);
assert.equal(clock.elements.date.textContent,'Niedziela, 04.10.2026');
clock.update('2026-10-04T10:59:00Z');
assert.equal(clock.elements.clock.textContent,'12:59'); // Re-read wall time after a long pause.
const noHighlight=fixture('2026-10-04T09:11:00Z',{highlightOnes:false});
assert.equal(noHighlight.elements.clock.children.filter(c=>c.className==='one').length,0);
const spring=fixture('2026-03-29T00:59:00Z');
assert.equal(spring.elements.clock.textContent,'01:59');
spring.update('2026-03-29T01:00:00Z');
assert.equal(spring.elements.clock.textContent,'03:00');
const autumn=fixture('2026-10-25T00:59:00Z');
assert.equal(autumn.elements.clock.textContent,'02:59');
autumn.update('2026-10-25T01:00:00Z');
assert.equal(autumn.elements.clock.textContent,'02:00');
const unsafe=fixture('2026-10-04T00:00:00Z',{icon:'../../remote',clockColor:'invalid'});
assert.equal(unsafe.elements.weatherIcon.src,'weather-icons/unknown.svg');
assert.equal(unsafe.elements.clock.style.color,'#FFFFFF');
const embedded=fixture('2026-10-04T09:15:00Z',{temperatureText:'18.4 °C',icon:'rainy'},true);
assert.equal(embedded.elements.tempValue.textContent,'18.4 °C');
assert.equal(embedded.elements.weatherIcon.src,'weather-icons/rainy.svg');
const info=fixture('2026-10-04T09:15:00Z',{info1Visible:true,info1Name:'Brama <test>',info1Icon:'gate',info1State:'Zamknięta',info1Tone:'quiet',info1IconSize:150,info1TextSize:80,info5Visible:true,info5Name:'Drzwi',info5Icon:'unknown',info5State:'Brak danych',info5Tone:'unknown'});
assert.equal(info.elements.indicators.children.length,2);
assert.equal(info.elements.indicators.children[0].children[1].textContent,'Brama <test>');
assert.equal(info.elements.indicators.children[0].children[0].style.width,'60px');
assert.equal(info.elements.indicators.children[1].children.length,2);
assert.equal(info.elements.indicators.children[1].children[0].alt,'Brak danych');
const customIcon=fixture('2026-10-04T09:15:00Z',{info1Visible:true,info1Name:'Czujnik',info1Path:'M0 0L24 24Z',info1Tone:'active',info1ActiveColor:'#00FF00',info1InactiveColor:'#112233'});
assert(Buffer.from(customIcon.elements.indicators.children[0].children[0].src.split(',')[1],'base64').toString().includes('fill="#00FF00"'));
const inactive=fixture('2026-10-04T09:15:00Z',{info1Visible:true,info1Path:'M0 0L24 24Z',info1Tone:'quiet',info1InactiveColor:'#112233'});
assert(Buffer.from(inactive.elements.indicators.children[0].children[0].src.split(',')[1],'base64').toString().includes('fill="#112233"'));
console.log('PASS: actual renderer, Warsaw time, Polish date, HH-only ones, resume, DST, safe asset paths');
const rail=fixture('2026-10-04T09:15:00Z',{infoPosition:'left',info1Visible:true,info1Name:'Brama'});
assert.equal(rail.elements.dashboard.className,'dashboard left');
const emptyRail=fixture('2026-10-04T09:15:00Z',{infoPosition:'left'});
assert.equal(emptyRail.elements.dashboard.className,'dashboard');
const gate=fixture('2026-10-04T09:15:00Z',{info1Visible:true,info1Tone:'alert',info1ActiveColor:'#123456',info1CustomSvg:'<svg viewBox="0 0 64 64" stroke="currentColor"><path d="M57 18V50"/></svg>'});
const gateSvg=Buffer.from(gate.elements.indicators.children[0].children[0].src.split(',')[1],'base64').toString();
assert(gateSvg.includes('stroke="#123456"'));
assert(gateSvg.includes('viewBox="0 0 64 64"'));

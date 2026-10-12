// Regression: native select input bubbles before change. The selected icon
// must reach the model before the card refresh synchronizes the dropdown.
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const path=require('node:path');
const template=fs.readFileSync(path.join(__dirname,'icon-configurator.html'),'utf8');
const generated=fs.readFileSync(path.join(__dirname,'../konfigurator-ikon.html'),'utf8');
assert(!generated.includes('%CUSTOM_JSON%'));
const start=template.indexOf(' const applyPicker=');
const end=template.indexOf(' pickerLabel.appendChild',start);
assert(start>0&&end>start);
const handlers={};
const input={value:''};
const custom=JSON.parse(fs.readFileSync(path.join(__dirname,'../resources/custom-icons.json'),'utf8'));
const picker={value:'',addEventListener(event,handler){handlers[event]=handler;}};
let rendered='';
function update(){
 const value=input.value.trim().replace(/^mdi:/,'');
 picker.value=custom[value]?value:'';
 rendered=custom[value]?custom[value].open:'';
}
vm.runInNewContext(template.slice(start,end),{picker,el:{querySelector(){return input;}},update});
for(const key of Object.keys(custom)){
 picker.value=key;
 handlers.input(); // select target listener
 update(); // bubbled input refresh on the parent card
 handlers.change();
 assert.equal(input.value,key);
 assert.equal(picker.value,key);
 assert.equal(rendered,custom[key].open);
}
picker.value='';handlers.input();update();handlers.change();
assert.equal(input.value,'');
assert.equal(rendered,'');
assert(generated.includes(template.slice(start,end)));
console.log('PASS: all custom icons survive input/change order, preview selection and clearing; generated helper current');

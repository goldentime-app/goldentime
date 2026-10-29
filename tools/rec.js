const {chromium}=require('playwright');
const CSS=`body{padding:0!important;background:#05070a!important}.wrap{max-width:none!important;gap:0!important}.top,.caption,aside{position:absolute!important;left:-99999px!important}
.stage{display:block!important}.unit{padding:0!important;border-radius:0!important;box-shadow:none!important}.display{border-radius:0!important}`;
const clips={
 c3:{scn:'A',plan:[[0,700],[1,700],[2,700],[3,700],[4,600],[5,5800]]},
 c4:{scn:'A',pre:7,plan:[[7,7000]]},
 c6:{scn:'A',pre:10,plan:[[10,7200]]},
 c9:{scn:'D',plan:[[0,2600],[2,2800]]}
};
(async()=>{const b=await chromium.launch({executablePath:'/opt/pw-browsers/chromium-1194/chrome-linux/chrome',args:['--headless=new'],ignoreDefaultArgs:['--headless=old']});
for(const [name,c] of Object.entries(clips)){ if(process.argv[4]&&!process.argv[4].split(',').includes(name)) continue;
 const ctx=await b.newContext({viewport:{width:1920,height:720}});const dir=process.argv[3]+'/'+name;require('fs').mkdirSync(dir,{recursive:true});let n=0;const log=[];
 const p=await ctx.newPage();await p.goto('file://'+process.argv[2]);await p.addStyleTag({content:CSS});
 await p.evaluate(s=>{const e=document.querySelector('#scn-sel');e.value=s;e.dispatchEvent(new Event('change'));},c.scn);
 // fast-forward hidden steps before recording window
 for(let i=0;i<(c.pre||0);i++) await p.evaluate(()=>document.querySelector('#next').click());
 await p.waitForTimeout(800);
 const t0=Date.now(); console.log(name,'mark',0);
 for(const [step,ms] of c.plan){await p.evaluate(i=>document.querySelectorAll('#steps li')[i].click(),step);const end=Date.now()+ms;
   while(Date.now()<end){const t=Date.now();const f=dir+'/f'+String(n).padStart(4,'0')+'.jpg';await p.screenshot({path:f,type:'jpeg',quality:88});log.push([f,t]);n++;}}
 const fs=require('fs');let txt='';for(let i=0;i<log.length;i++){const d=((i+1<log.length?log[i+1][1]:Date.now())-log[i][1])/1000;txt+=`file '${log[i][0]}'\nduration ${d.toFixed(3)}\n`;}txt+=`file '${log[log.length-1][0]}'\n`;fs.writeFileSync(dir+'/list.txt',txt);console.log(name,n,'frames');
 await ctx.close();
}
await b.close();})();

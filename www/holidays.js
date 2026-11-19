// Lịch nghỉ lễ/Tết Việt Nam tự tính 2026–2046 (âm lịch theo thuật toán Hồ Ngọc Đức, múi giờ +7)
(function(){
  const PI=Math.PI, INT=Math.floor;
  function jdFromDate(dd,mm,yy){const a=INT((14-mm)/12),y=yy+4800-a,m=mm+12*a-3;let jd=dd+INT((153*m+2)/5)+365*y+INT(y/4)-INT(y/100)+INT(y/400)-32045;if(jd<2299161)jd=dd+INT((153*m+2)/5)+365*y+INT(y/4)-32083;return jd;}
  function jdToDate(jd){let a,b,c;if(jd>2299160){a=jd+32044;b=INT((4*a+3)/146097);c=a-INT((b*146097)/4);}else{b=0;c=jd+32082;}const d=INT((4*c+3)/1461),e=c-INT((1461*d)/4),m=INT((5*e+2)/153);return[e-INT((153*m+2)/5)+1,m+3-12*INT(m/10),b*100+d-4800+INT(m/10)];}
  function newMoon(k){const T=k/1236.85,T2=T*T,T3=T2*T,dr=PI/180;let Jd1=2415020.75933+29.53058868*k+0.0001178*T2-0.000000155*T3;Jd1+=0.00033*Math.sin((166.56+132.87*T-0.009173*T2)*dr);const M=359.2242+29.10535608*k-0.0000333*T2-0.00000347*T3,Mpr=306.0253+385.81691806*k+0.0107306*T2+0.00001236*T3,F=21.2964+390.67050646*k-0.0016528*T2-0.00000239*T3;let C1=(0.1734-0.000393*T)*Math.sin(M*dr)+0.0021*Math.sin(2*dr*M)-0.4068*Math.sin(Mpr*dr)+0.0161*Math.sin(dr*2*Mpr)-0.0004*Math.sin(dr*3*Mpr)+0.0104*Math.sin(dr*2*F)-0.0051*Math.sin(dr*(M+Mpr))-0.0074*Math.sin(dr*(M-Mpr))+0.0004*Math.sin(dr*(2*F+M))-0.0004*Math.sin(dr*(2*F-M))-0.0006*Math.sin(dr*(2*F+Mpr))+0.0010*Math.sin(dr*(2*F-Mpr))+0.0005*Math.sin(dr*(2*Mpr+M));let deltat=T<-11?0.001+0.000839*T+0.0002261*T2-0.00000845*T3-0.000000081*T*T3:-0.000278+0.000265*T+0.000262*T2;return Jd1+C1-deltat;}
  function sunLong(jdn){const T=(jdn-2451545.0)/36525,T2=T*T,dr=PI/180,M=357.52910+35999.05030*T-0.0001559*T2-0.00000048*T*T2,L0=280.46645+36000.76983*T+0.0003032*T2;let DL=(1.914600-0.004817*T-0.000014*T2)*Math.sin(dr*M)+(0.019993-0.000101*T)*Math.sin(dr*2*M)+0.000290*Math.sin(dr*3*M);let L=(L0+DL)*dr;L=L-PI*2*INT(L/(PI*2));return L;}
  const TZ=7;
  const getNewMoonDay=k=>INT(newMoon(k)+0.5+TZ/24);
  const getSunLongitude=jd=>INT(sunLong(jd-0.5-TZ/24)/PI*6);
  function lunarMonth11(yy){const off=jdFromDate(31,12,yy)-2415021,k=INT(off/29.530588853);let nm=getNewMoonDay(k);if(getSunLongitude(nm)>=9)nm=getNewMoonDay(k-1);return nm;}
  function leapOffset(a11){const k=INT((a11-2415021.076998695)/29.530588853+0.5);let last=0,i=1,arc=getSunLongitude(getNewMoonDay(k+i));do{last=arc;i++;arc=getSunLongitude(getNewMoonDay(k+i));}while(arc!=last&&i<14);return i-1;}
  // âm lịch (không nhuận) -> dương lịch [d,m,y]
  function lunar2solar(ld,lm,ly){let a11,b11;if(lm<11){a11=lunarMonth11(ly-1);b11=lunarMonth11(ly);}else{a11=lunarMonth11(ly);b11=lunarMonth11(ly+1);}const k=INT(0.5+(a11-2415021.076998695)/29.530588853);let off=lm-11;if(off<0)off+=12;if(b11-a11>365){const lo=leapOffset(a11);let lp=lo-2;if(lp<0)lp+=12;if(off>=lo)off+=1;}return jdToDate(getNewMoonDay(k+off)+ld-1);}

  // Lịch chính thức đã công bố
  const OFFICIAL={
    2026:['01/01','02/01','16/02','17/02','18/02','19/02','20/02','27/04','30/04','01/05','31/08','01/09','02/09','24/11'],
    2027:['01/01','04/02','05/02','08/02','09/02','10/02','16/04','30/04','03/05','02/09','03/09','24/11']
  };
  const key=(y,m,d)=>`${y}-${m}-${d}`;
  function fromJd(jd){const [d,m,y]=jdToDate(jd);return [y,m,d];}
  const dow=jd=>(jd+1)%7; // 0=CN … 6=T7
  function ruleYear(y){
    const days=new Map(); // jd -> tên
    const add=(jd,n)=>{ if(!days.has(jd)) days.set(jd,n); };
    add(jdFromDate(1,1,y),'Tết Dương lịch');
    const [td,tm,ty]=lunar2solar(1,1,y), t1=jdFromDate(td,tm,ty);
    for(let i=-2;i<=2;i++) add(t1+i,'Tết Nguyên đán');
    const [gd,gm,gy]=lunar2solar(10,3,y); add(jdFromDate(gd,gm,gy),'Giỗ Tổ Hùng Vương');
    add(jdFromDate(30,4,y),'Giải phóng miền Nam'); add(jdFromDate(1,5,y),'Quốc tế Lao động');
    const q=jdFromDate(2,9,y); add(q,'Quốc khánh');
    const qd=dow(q); add(qd===2? q-1 : (qd===5? q+1 : (qd===1? q+1 : (qd===6? q-1 : q+1))),'Quốc khánh');
    add(jdFromDate(24,11,y),'Ngày Văn hóa Việt Nam');
    // nghỉ bù: ngày lễ trùng T7/CN -> nghỉ bù ngày làm việc kế tiếp
    const base=[...days.keys()].sort((a,b)=>a-b);
    for(const jd of base){ const w=dow(jd); if(w===0||w===6){ let n=jd+1; while(dow(n)===0||dow(n)===6||days.has(n)) n++; days.set(n,'Nghỉ bù'); } }
    return days;
  }
  // Trả về Map "y-m-d" -> tên, cho 1 năm
  window.vnHolidays=function(y){
    const out=new Map();
    if(OFFICIAL[y]) OFFICIAL[y].forEach(s=>{const [d,m]=s.split('/').map(Number); out.set(key(y,m,d),'Lễ/Tết (lịch chính thức)');});
    else ruleYear(y).forEach((n,jd)=>{const [yy,m,d]=fromJd(jd); if(yy===y) out.set(key(yy,m,d),n);});
    return out;
  };
  window.vnLunar2Solar=lunar2solar;
  window.VN_HOL_OFFICIAL_YEARS=Object.keys(OFFICIAL).map(Number);
})();

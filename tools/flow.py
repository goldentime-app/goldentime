from PIL import Image, ImageDraw, ImageFont
import math
W,H=2400,1300
im=Image.new('RGB',(W,H),'white'); d=ImageDraw.Draw(im)
F=lambda s,b=False: ImageFont.truetype("/root/.fonts/Pretendard-%s.otf"%("Bold" if b else "Medium"),s)
C={'일상':('#E6F7F3','#35C2A7'),'주의':('#FDF3E3','#F2A93B'),'응급':('#FDECEC','#E5484D'),'회색':('#F1F4F7','#8A97A4')}
def box(x,y,t,k,w=330,h=110):
    f,o=C[k]; d.rounded_rectangle([x,y,x+w,y+h],radius=18,fill=f,outline=o,width=4)
    ls=t.split('\n'); 
    for i,l in enumerate(ls): d.text((x+w/2,y+h/2+(i-(len(ls)-1)/2)*40),l,font=F(30,i==0),fill='#1b1f24',anchor='mm')
    return (x,y,w,h)
def arrow(p,q,label=None,off=(0,-24)):
    d.line([p,q],fill='#55606b',width=4); a=math.atan2(q[1]-p[1],q[0]-p[0]); L=20
    d.polygon([q,(q[0]-L*math.cos(a-0.4),q[1]-L*math.sin(a-0.4)),(q[0]-L*math.cos(a+0.4),q[1]-L*math.sin(a+0.4))],fill='#55606b')
    if label:
        cx,cy=(p[0]+q[0])/2+off[0],(p[1]+q[1])/2+off[1]
        bb=d.textbbox((cx,cy),label,font=F(24),anchor='mm'); d.rectangle([bb[0]-8,bb[1]-5,bb[2]+8,bb[3]+5],fill='white')
        d.text((cx,cy),label,font=F(24),fill='#55606b',anchor='mm')
def R(b): return (b[0]+b[2],b[1]+b[3]/2)
def Lf(b): return (b[0],b[1]+b[3]/2)
def T(b): return (b[0]+b[2]/2,b[1])
def B(b): return (b[0]+b[2]/2,b[1]+b[3])
h0=box(60,560,"H0 홈","일상")
h2=box(520,120,"H2 멀미 대응","일상"); hw=box(980,120,"휴게소 경유","회색")
h3=box(520,340,"H3 주의 모드","주의"); h3w=box(980,340,"약국·야간진료 경유","회색")
e7=box(520,900,"E7 사고 자동 감지","응급")
e1=box(980,620,"E1 응급 접수","응급"); e2=box(1440,620,"E2 요약·119","응급")
e6=box(1900,620,"E6 심정지 의심\n정차·119·압박","응급",h=130)
e3=box(1440,900,"E3 이동 방법 비교\n(랑데부)","응급",h=130)
e4=box(1900,880,"E4 접선 지점 이동","응급"); e5=box(1900,1120,"E5 도착·인계","응급")
arrow(R(h0),Lf(h2),"멀미 위험 70%↑",(-60,0)); arrow(R(h2),Lf(hw),"동의")
arrow(R(h0),Lf(h3),"증상 발화",(40,10)); arrow(R(h3),Lf(h3w),"선택")
arrow(B(h3),(1100,620),"“응급 모드”",(60,0))
arrow(R(h0),Lf(e1),"응급 버튼·음성·Gleo",(-20,-30))
arrow(R(h0),Lf(e7),"에어백 전개",(-70,0)); arrow(T(e7),(1100,730),"무응답 10초",(-90,0))
arrow(R(e1),Lf(e2)); arrow(R(e2),Lf(e6),"숨 안 쉼·의식 없음",(0,-50))
arrow(B(e2),T(e3),"심정지 아님",(80,0)); arrow(R(e3),Lf(e4),"접선",(0,-24))
arrow(B(e4),T(e5),"반경 진입·정차",(110,0)); arrow((e3[0]+e3[2]/2,e3[1]+e3[3]),(e5[0],e5[1]+55),"대기·직접 이동",(-60,40))
d.text((60,60),"골든타임 화면 흐름",font=F(44,True),fill='#1b1f24')
for i,(k,lab) in enumerate((('일상','일상'),('주의','주의'),('응급','응급'))):
    f,o=C[k]; d.rounded_rectangle([60+i*170,1200,90+i*170,1230],radius=6,fill=f,outline=o,width=3); d.text((100+i*170,1215),lab,font=F(26),fill='#1b1f24',anchor='lm')
im.save('flow.png'); print('ok')

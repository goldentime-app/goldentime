from PIL import Image, ImageDraw, ImageFont
W,H=1920,720
FR="/root/.fonts/Pretendard-Regular.otf"; FB="/root/.fonts/Pretendard-Bold.otf"; FM="/root/.fonts/Pretendard-Medium.otf"
def f(s,w='r'): return ImageFont.truetype({'r':FR,'b':FB,'m':FM}[w],s)
BG='#0E1217'; CARD='#1A2129'; CARD2='#232C36'; LINE='#2E3944'; TXT='#F3F5F7'; SUB='#9AA6B2'
TEAL='#35C2A7'; RED='#E5484D'; REDBG='#2A1416'; AMB='#F2A93B'; BLUE='#4C8DF6'
class S:
    def __init__(s,title,mode='일상',bg=BG,accent=TEAL,driving=True):
        s.im=Image.new('RGB',(W,H),bg); s.d=ImageDraw.Draw(s.im)
        d=s.d
        d.rectangle([0,0,W,64],fill='#0A0D11' if bg==BG else '#1E0B0D')
        d.text((32,32),"골든타임",font=f(26,'b'),fill=TXT,anchor='lm')
        d.rounded_rectangle([170,16,170+24+len(mode)*26,48],radius=16,fill=accent)
        d.text((182,32),mode,font=f(20,'b'),fill='#0A0D11',anchor='lm')
        d.text((W-32,32),"14:32  ·  주행 중" if driving else "14:32  ·  정차",font=f(22),fill=SUB,anchor='rm')
        s.title=title
    def card(s,x,y,w,h,fill=CARD,r=22,outline=None):
        s.d.rounded_rectangle([x,y,x+w,y+h],radius=r,fill=fill,outline=outline,width=3 if outline else 0)
    def t(s,x,y,txt,size=28,w='r',c=TXT,a='la'):
        s.d.text((x,y),txt,font=f(size,w),fill=c,anchor=a)
    def btn(s,x,y,w,h,txt,fill=RED,c='white',size=34):
        s.d.rounded_rectangle([x,y,x+w,y+h],radius=h//2,fill=fill)
        s.d.text((x+w/2,y+h/2),txt,font=f(size,'b'),fill=c,anchor='mm')
    def gauge(s,cx,cy,r,val,color,label):
        d=s.d; d.arc([cx-r,cy-r,cx+r,cy+r],start=135,end=405,fill=LINE,width=22)
        d.arc([cx-r,cy-r,cx+r,cy+r],start=135,end=135+270*val/100,fill=color,width=22)
        d.text((cx,cy-8),str(val),font=f(72,'b'),fill=TXT,anchor='mm'); d.text((cx,cy+52),label,font=f(24),fill=SUB,anchor='mm')
    def mic(s,cx,cy,r=60,color=RED):
        d=s.d
        for k,a in ((r+50,'#3A1A1D'),(r+25,'#5A2025')): d.ellipse([cx-k,cy-k,cx+k,cy+k],fill=a)
        d.ellipse([cx-r,cy-r,cx+r,cy+r],fill=color)
        d.rounded_rectangle([cx-14,cy-30,cx+14,cy+10],radius=14,fill='white'); d.arc([cx-24,cy-14,cx+24,cy+24],0,180,fill='white',width=5); d.line([cx,cy+24,cx,cy+36],fill='white',width=5)
    def map(s,x,y,w,h,route=True,pin=None):
        d=s.d; s.card(x,y,w,h,fill='#141B22')
        for i in range(1,6): d.line([x+i*w/6,y+10,x+i*w/6,y+h-10],fill='#1B242D',width=2)
        for i in range(1,4): d.line([x+10,y+i*h/4,x+w-10,y+i*h/4],fill='#1B242D',width=2)
        if route:
            pts=[(x+60,y+h-60),(x+w*0.3,y+h*0.6),(x+w*0.5,y+h*0.62),(x+w*0.75,y+h*0.35),(x+w-60,y+60)]
            d.line(pts,fill=BLUE,width=12,joint='curve')
            d.ellipse([pts[0][0]-16,pts[0][1]-16,pts[0][0]+16,pts[0][1]+16],fill='white',outline=BLUE,width=6)
            if pin:
                px,py=pts[pin]; d.ellipse([px-22,py-22,px+22,py+22],fill=AMB); d.text((px,py-46),"접선",font=f(22,'b'),fill=AMB,anchor='mm')
            ex,ey=pts[-1]; d.rounded_rectangle([ex-22,ey-22,ex+22,ey+22],radius=6,fill=RED); d.text((ex,ey),"+",font=f(34,'b'),fill='white',anchor='mm')
    def caption(s,txt):
        s.d.rectangle([0,H-56,W,H],fill='#06080B'); s.d.text((32,H-28),txt,font=f(22),fill=SUB,anchor='lm')
    def save(s,fn): s.im.save(fn)

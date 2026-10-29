import os, re, subprocess, sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import imageio_ffmpeg
FF = imageio_ffmpeg.get_ffmpeg_exe()
SP = '/tmp/claude-0/-home-user-sero-term/d2170753-3114-53c2-bacc-19fa14ae5b0a/scratchpad/shorts'
OUT = SP + '/seg'; os.makedirs(OUT, exist_ok=True)
W, H, FPS = 1080, 1920, 30
B = '/root/.fonts/Pretendard-Bold.otf'
R = B.replace('Bold', 'Regular') if os.path.exists(B.replace('Bold', 'Regular')) else B
def F(sz, bold=True): return ImageFont.truetype(B if bold else R, sz)
INK, SUB, TEAL, RED, AMB = (238, 242, 245), (154, 167, 180), (55, 195, 166), (229, 72, 77), (240, 168, 58)

def text_block(d, lines, y, size, fill=INK, bold=True, gap=1.25, shadow=False):
    f = F(size, bold)
    for ln in lines:
        w = d.textlength(ln, font=f)
        x = (W - w) / 2
        if shadow:
            for dx, dy in ((3, 3), (0, 3), (3, 0)): d.text((x + dx, y + dy), ln, font=f, fill=(0, 0, 0))
        d.text((x, y), ln, font=f, fill=fill)
        y += int(size * gap)
    return y

def wrap(txt, size, maxw, bold=False):
    f = F(size, bold); d = ImageDraw.Draw(Image.new('RGB', (1, 1)))
    out, cur = [], ''
    for ch in txt.split(' '):
        t = (cur + ' ' + ch).strip()
        if d.textlength(t, font=f) > maxw and cur: out.append(cur); cur = ch
        else: cur = t
    out.append(cur); return out

def overlay(head=None, sub=None, tag=None, head_color=INK, on_photo=False):
    o = Image.new('RGBA', (W, H), (0, 0, 0, 0)); d = ImageDraw.Draw(o)
    if on_photo:  # gradients for legibility
        g = Image.new('L', (1, H))
        for y in range(H):
            a = max(0, 1 - y / 700) * 200 if y < 700 else max(0, (y - 1350) / 570) * 210
            g.putpixel((0, y), int(a))
        o.paste(Image.new('RGBA', (W, H), (0, 0, 0, 255)), (0, 0), g.resize((W, H)))
        d = ImageDraw.Draw(o)
    if head: text_block(d, head, 230, 78, head_color, shadow=on_photo)
    if sub:
        lines = wrap(sub, 46, 920)
        text_block(d, lines, 1420 if not on_photo else 1560, 46, INK, bold=False, gap=1.4, shadow=on_photo)
    if tag:
        f = F(28, False); tw = d.textlength(tag, font=f)
        d.rounded_rectangle((W - tw - 70, 1800, W - 30, 1850), 25, fill=(0, 0, 0, 150)); d.text((W - tw - 50, 1806), tag, font=f, fill=SUB)
    return o

def encode(frames_dir, name, list_file=None):
    out = f'{OUT}/{name}.mp4'
    if list_file:
        cmd = [FF, '-y', '-loglevel', 'error', '-f', 'concat', '-safe', '0', '-i', list_file]
    else:
        cmd = [FF, '-y', '-loglevel', 'error', '-framerate', str(FPS), '-i', frames_dir + '/%04d.jpg']
    cmd += ['-vf', f'fps={FPS},format=yuv420p', '-c:v', 'libx264', '-preset', 'medium', '-crf', '20', out]
    subprocess.run(cmd, check=True); return out

def still(name, img, secs, head=None, sub=None, focus=(0.5, 0.5), z0=1.0, z1=1.15, head_color=INK):
    src = Image.open(img).convert('RGB')
    s = max(W / src.width, H / src.height); src = src.resize((int(src.width * s) + 1, int(src.height * s) + 1), Image.LANCZOS)
    ov = overlay(head, sub, 'AI 생성 연출', head_color, on_photo=True)
    d = f'{OUT}/{name}'; os.makedirs(d, exist_ok=True)
    n = int(secs * FPS)
    for i in range(n):
        t = i / max(1, n - 1); t = t * t * (3 - 2 * t)
        z = z0 + (z1 - z0) * t
        cw, ch = W / z, H / z
        cx = (src.width / 2) + (focus[0] * src.width - src.width / 2) * (z - 1) / max(1e-6, z1 - 1)
        cy = (src.height / 2) + (focus[1] * src.height - src.height / 2) * (z - 1) / max(1e-6, z1 - 1)
        x0 = min(max(0, cx - cw / 2), src.width - cw); y0 = min(max(0, cy - ch / 2), src.height - ch)
        fr = src.resize((W, H), Image.LANCZOS, box=(x0, y0, x0 + cw, y0 + ch)).convert('RGBA')
        fr.alpha_composite(ov); fr.convert('RGB').save(f'{d}/{i:04d}.jpg', quality=90)
    return encode(d, name)

def screen(name, rec_dir, head, sub, head_color=INK, pan=(0.5, 0.5), zoom=1.5):
    ov = overlay(head, sub, '프로토타입 실제 화면 · 시연용 수치', head_color)
    lst = open(rec_dir + '/list.txt').read()
    d = f'{OUT}/{name}'; os.makedirs(d, exist_ok=True)
    total = len(re.findall(r"file '", lst)); k = [0]
    sw, sh = int(W * zoom), int(W * zoom * 720 / 1920)
    def conv(m):
        p = m.group(1); out = f'{d}/{os.path.basename(p)}'
        t = k[0] / max(1, total - 1); k[0] += 1; t = t * t * (3 - 2 * t)
        fr = Image.new('RGBA', (W, H), (5, 7, 10, 255))
        sc = Image.open(p).convert('RGB').resize((sw, sh), Image.LANCZOS)
        x = int((pan[0] + (pan[1] - pan[0]) * t) * (sw - W))
        y = 960 - sh // 2; fr.paste(sc.crop((x, 0, x + W, sh)), (0, y))
        fr.alpha_composite(ov); fr.convert('RGB').save(out, quality=90)
        return f"file '{out}'"
    new = re.sub(r"file '([^']+)'", conv, lst)
    open(d + '/list.txt', 'w').write(new)
    return encode(d, name, d + '/list.txt')

def endcard(name, secs=4):
    d = f'{OUT}/{name}'; os.makedirs(d, exist_ok=True)
    n = int(secs * FPS)
    for i in range(n):
        a = min(1, i / 12)
        fr = Image.new('RGB', (W, H), (11, 14, 18)); dr = ImageDraw.Draw(fr)
        dr.rounded_rectangle((W / 2 - 60, 620, W / 2 + 60, 740), 30, fill=tuple(int(c * a) for c in RED))
        dr.text((W / 2, 680), '+', font=F(90), fill=(255, 255, 255), anchor='mm')
        col = lambda c: tuple(int(11 + (x - 11) * a) for x in c)
        text_block(dr, ['골든타임'], 800, 120, col(INK))
        text_block(dr, ['이동 중 건강·응급 AI 코파일럿'], 960, 50, col(SUB), bold=False)
        text_block(dr, ['랑데부 내비 · 멀미 예보 · 사고 자동 감지'], 1100, 40, col(TEAL), bold=False)
        text_block(dr, ['Pleos Connect SDK 기반', '119가 우선 · 진단하지 않습니다'], 1560, 36, col(SUB), bold=False, gap=1.5)
        fr.save(f'{d}/{i:04d}.jpg', quality=90)
    return encode(d, name)

if __name__ == '__main__':
    AI, REC = SP + '/ai', SP + '/rec'
    segs = []
    which = sys.argv[1:] or ['all']
    plan = [
      ('01', lambda: still('01', AI + '/s1.png', 3.2, ['고속도로 한복판,', '아이가 경련한다면?'], focus=(0.57, 0.38), z1=1.25)),
      ('02', lambda: still('02', AI + '/s2.png', 3.3, None, '“아이가 경련해!”', focus=(0.5, 0.4), z0=1.05, z1=1.2)),
      ('03', lambda: screen('03', REC + '/c3', ['말 한마디로 응급 모드'], '“지금 119에 연결하세요. 화면의 요약을 그대로 읽어 주시면 됩니다.”', pan=(0.75, 0.15))),
      ('04', lambda: screen('04', REC + '/c4', ['기다리면 22분', '만나러 가면 9분'], '“8km 앞 졸음쉼터에서 구급차를 만나면 9분 안에 의료진을 만날 수 있어요.”', TEAL, pan=(0.0, 1.0))),
      ('05', lambda: still('05', AI + '/s5.png', 3.8, ['구급차와 만날 곳을', '차가 계산합니다'], focus=(0.5, 0.5), z1=1.18)),
      ('06', lambda: screen('06', REC + '/c6', ['도착하면 문을 열고', '정보를 넘겨요'], '“문 잠금을 풀었어요. 구급대원에게 화면을 보여 주세요.”', pan=(0.0, 0.9))),
      ('07', lambda: still('07', AI + '/s8.png', 3.5, ['평소에는', '멀미까지'], focus=(0.5, 0.45), z1=1.15)),
      ('08', lambda: screen('08', REC + '/c9', ['논문 기반 멀미 예측', '회전 전에 미리 알림'], '“곧 오른쪽으로 돌아요.”', AMB, pan=(0.2, 0.0))),
      ('09', lambda: endcard('09')),
    ]
    for k, fn in plan:
        if 'all' in which or k in which: print(fn())

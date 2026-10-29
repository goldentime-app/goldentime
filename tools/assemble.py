import subprocess, re, wave, numpy as np, imageio_ffmpeg, os
FF = imageio_ffmpeg.get_ffmpeg_exe()
SP = '/tmp/claude-0/-home-user-sero-term/d2170753-3114-53c2-bacc-19fa14ae5b0a/scratchpad/shorts'
SR = 48000
segs = ['01','02','03','04','05','06','07','08','09']
def dur(p):
    r = subprocess.run([FF, '-i', p], capture_output=True, text=True).stderr
    h, m, s = re.search(r'Duration: (\d+):(\d+):([\d.]+)', r).groups(); return int(h)*3600+int(m)*60+float(s)
D = [dur(f'{SP}/seg/{s}.mp4') for s in segs]
start = np.cumsum([0]+D[:-1]); total = sum(D)
print([round(x,2) for x in D], round(total,2))
# voice placement: (segment, file, offset within segment, gain)
voices = [('02','a2.wav',0.25,1.0),('03','a3.wav',3.5,1.0),('04','a4.wav',0.6,1.0),('06','a6.wav',0.3,1.0),('08','a8.wav',2.7,1.0)]
def load(p):
    r = subprocess.run([FF,'-loglevel','error','-i',p,'-f','f32le','-ac','1','-ar',str(SR),'-'],capture_output=True,check=True).stdout
    return np.frombuffer(r, dtype=np.float32)
N = int(total*SR)+SR; mix = np.zeros(N, np.float32)
for seg, f, off, g in voices:
    p = f'{SP}/aud/{f}'
    if not os.path.exists(p): print('missing', f); continue
    a = load(p); a = a/ (np.abs(a).max()+1e-6) * 0.85 * g
    i = int((start[segs.index(seg)]+off)*SR); mix[i:i+len(a)] += a[:N-i]
# synthesized ambient pad: tense minor until segment 07, warm major after
t = np.arange(N)/SR
def chord(freqs, env):
    s = sum(np.sin(2*np.pi*f*t + 0.3*np.sin(2*np.pi*0.1*t*k)) for k,f in enumerate(freqs,1))
    return s/len(freqs)*env
cut = start[segs.index('07')]
fade = lambda a,b: np.clip((t-a)/max(b-a,1e-6),0,1)
env_tense = (fade(0,1.5))*(1-fade(cut-1,cut))
env_warm = fade(cut-1,cut)*(1-fade(total-1.5,total))
pad = chord([110,130.81,164.81,220],env_tense)*0.10 + chord([130.81,164.81,196,261.63],env_warm)*0.10
pulse = (np.sin(2*np.pi*1.6*t)>0.95).astype(np.float32)*np.sin(2*np.pi*55*t)*env_tense*0.08  # heartbeat-like
# duck pad under voices
voice_env = np.convolve(np.abs(mix), np.ones(SR//10)/(SR//10), mode='same')
duck = 1 - 0.6*np.clip(voice_env*8,0,1)
out = mix + (pad+pulse)*duck
out = out/ max(1, np.abs(out).max()/0.95)
with wave.open(f'{SP}/mix.wav','wb') as w:
    w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes((out*32767).astype(np.int16).tobytes())
open(f'{SP}/concat.txt','w').write(''.join(f"file '{SP}/seg/{s}.mp4'\n" for s in segs))
final = f'{SP}/골든타임_쇼츠_초안.mp4'
subprocess.run([FF,'-y','-loglevel','error','-f','concat','-safe','0','-i',f'{SP}/concat.txt','-i',f'{SP}/mix.wav',
  '-c:v','copy','-c:a','aac','-b:a','192k','-shortest','-movflags','+faststart',final],check=True)
print(final, round(dur(final),2))

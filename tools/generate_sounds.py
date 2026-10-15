#!/usr/bin/env python3
"""Gera os sons EXTRAS originais do Cannon Game (win_jingle, lose_jingle, bg_music).
Os 3 sons do capítulo (cannon_fire, target_hit, blocker_hit) vêm do material da disciplina.
Uso: python3 tools/generate_sounds.py  (requer numpy)"""
import wave, numpy as np
SR = 22050
OUT = "app/src/main/res/raw/"

def freq(n):  # n = semitons a partir de A4
    return 440.0 * 2 ** (n / 12.0)

def tone(f, dur, kind="square", vol=0.5, decay=4.0):
    t = np.arange(int(SR * dur)) / SR
    ph = 2 * np.pi * f * t
    if kind == "square":
        w = np.sign(np.sin(ph))
    elif kind == "tri":
        w = 2 / np.pi * np.arcsin(np.sin(ph))
    else:
        w = np.sin(ph)
    env = np.exp(-decay * t / dur)
    env[:int(0.004 * SR)] *= np.linspace(0, 1, int(0.004 * SR))
    env[-int(0.01 * SR):] *= np.linspace(1, 0, int(0.01 * SR))
    return w * env * vol

def save(name, data):
    data = np.clip(data, -1, 1)
    pcm = (data * 32000).astype("<i2")
    with wave.open(OUT + name, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())

def place(buf, start, sig):
    i = int(start * SR); buf[i:i + len(sig)] += sig[:len(buf) - i]

# vitória: arpejo ascendente C5 E5 G5 C6
buf = np.zeros(int(SR * 1.2))
for k, n in enumerate([-9, -5, -2, 3]):
    place(buf, k * 0.14, tone(freq(n), 0.55 if k == 3 else 0.3, "square", 0.35, 3))
save("win_jingle.wav", buf)

# derrota: notas descendentes
buf = np.zeros(int(SR * 1.4))
for k, (n, d) in enumerate([(-2, 0.25), (-5, 0.25), (-9, 0.25), (-21, 0.6)]):
    place(buf, k * 0.25, tone(freq(n), d, "tri", 0.6, 2.5))
save("lose_jingle.wav", buf)

# música de fundo: 8 compassos, 140 bpm, Am - F - C - G (x2)
bpm = 140; beat = 60 / bpm; bars = 8
buf = np.zeros(int(SR * beat * 4 * bars))
prog = [(-12, [0, 3, 7]), (-16, [0, 4, 7]), (-21, [0, 4, 7]), (-14, [0, 4, 7])]  # raiz, acorde
mel = [[0, 3, 7, 3, 0, 3, 7, 12], [0, 4, 7, 4, 0, 4, 7, 12], [0, 4, 7, 4, 0, 4, 7, 9], [2, 7, 11, 7, 2, 7, 11, 14]]
for bar in range(bars):
    root, ch = prog[bar % 4]
    t0 = bar * 4 * beat
    for b in range(4):  # baixo
        place(buf, t0 + b * beat, tone(freq(root - 12), beat * 0.9, "tri", 0.45, 3))
    for s in range(8):  # melodia em colcheias
        n = root + 12 + mel[bar % 4][s]
        place(buf, t0 + s * beat / 2, tone(freq(n), beat / 2 * 0.9, "square", 0.12, 4))
buf /= max(1.0, np.abs(buf).max() / 0.8)
save("bg_music.wav", buf)
print("ok")

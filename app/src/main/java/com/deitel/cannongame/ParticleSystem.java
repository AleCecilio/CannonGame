// ParticleSystem.java
// EXTRA: partículas simples para a explosão ao acertar um alvo/blocker.
// Cada partícula é um pequeno círculo com movimento radial e fade out,
// atualizado quadro a quadro pelo mesmo game loop do CannonView.
package com.deitel.cannongame;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.Random;

public class ParticleSystem {
   private static final int MAX_PARTICLES = 300;
   private static final double DRAG_PER_SECOND = 1.8; // atrito

   private static class Particle {
      float x, y; // posição
      float vx, vy; // velocidade em pixels por segundo
      float radius;
      double life, maxLife; // vida restante / total, em segundos
      int color;
   }

   private final ArrayList<Particle> particles = new ArrayList<>();
   private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
   private final Random random = new Random();

   // cria uma explosão radial de 'count' partículas
   public synchronized void spawn(float x, float y, int color, int count,
      float speed, float radius) {
      for (int i = 0; i < count && particles.size() < MAX_PARTICLES; i++) {
         Particle p = new Particle();
         double angle = random.nextDouble() * 2 * Math.PI;
         double s = speed * (0.4 + 0.6 * random.nextDouble());
         p.x = x;
         p.y = y;
         p.vx = (float) (Math.cos(angle) * s);
         p.vy = (float) (Math.sin(angle) * s);
         p.radius = radius * (0.5f + random.nextFloat());
         p.maxLife = p.life = 0.5 + 0.4 * random.nextDouble();
         p.color = color;
         particles.add(p);
      }
   }

   // atualiza as posições; interval é o tempo do quadro em segundos
   public synchronized void update(double interval) {
      float drag = (float) Math.max(0, 1 - DRAG_PER_SECOND * interval);
      for (int i = particles.size() - 1; i >= 0; i--) {
         Particle p = particles.get(i);
         p.life -= interval;
         if (p.life <= 0) {
            particles.remove(i);
            continue;
         }
         p.x += p.vx * interval;
         p.y += p.vy * interval;
         p.vx *= drag;
         p.vy *= drag;
      }
   }

   // desenha as partículas com transparência proporcional à vida restante
   public synchronized void draw(Canvas canvas) {
      for (Particle p : particles) {
         paint.setColor(p.color);
         paint.setAlpha((int) (255 * Math.max(0, p.life / p.maxLife)));
         canvas.drawCircle(p.x, p.y, p.radius, paint);
      }
   }

   public synchronized void clear() {
      particles.clear();
   }
}

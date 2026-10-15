// Target.java
// Subclasse de GameElement personalizada para o Target (Fig. 6.9)
package com.deitel.cannongame;

import android.graphics.Canvas;
import android.graphics.Color;

public class Target extends GameElement {
   public static final double WAVE_FREQUENCY = 5.0; // frequência da onda senoidal
   public static final double WAVE_AMPLITUDE = 50.0; // amplitude do deslocamento X em pixels

   private final int hitReward; // recompensa de acerto deste alvo
   private final int initialX; // posição X original do alvo
   private double timeAlive = 0.0; // tempo de vida acumulado para o movimento senoidal
   private float alpha = 0f; // opacidade para o efeito de Fade In (0 a 255)
   private double lifetime = 0.0; // tempo acumulado para o pulso de escala
   private final int originalColor; // cor do modo dia
   private final int neonColor; // cor neon vibrante do modo noturno

   // construtor
   public Target(CannonView view, int color, int hitReward, int x, int y,
      int width, int length, float velocityY) {
      super(view, color, CannonView.TARGET_SOUND_ID, x, y, width, length,
         velocityY);
      this.hitReward = hitReward;
      this.initialX = x; // guarda a posição X inicial
      this.originalColor = color;
      // Define cor neon vibrante (CYAN / MAGENTA) para alto contraste no escuro
      this.neonColor = (color == view.getResources().getColor(R.color.dark, view.getContext().getTheme()))
         ? Color.CYAN : Color.MAGENTA;
      paint.setAlpha(0); // inicia transparente para o efeito de Fade In
   }

   // retorna a recompensa de acerto deste Target
   public int getHitReward() {
      return hitReward;
   }

   // atualiza a posição (movimento no eixo Y e onda senoidal no eixo X), tempo e opacidade
   @Override
   public void update(double interval) {
      super.update(interval); // preserva a lógica original do eixo Y e colisões com topo/base
      timeAlive += interval;
      lifetime += interval;

      // Cálculo da onda senoidal para a posição X
      int newX = initialX + (int) (Math.sin(timeAlive * WAVE_FREQUENCY) * WAVE_AMPLITUDE);
      shape.offsetTo(newX, shape.top);

      // aumenta gradualmente a opacidade até o limite de 255 (Fade In)
      if (alpha < 255f) {
         alpha = Math.min(255f, alpha + (float) (interval * 500));
         paint.setAlpha((int) alpha);
      }
   }

   // desenha o Target com cor ajustada ao tema e animação de pulso
   @Override
   public void draw(Canvas canvas) {
      int currentColor = view.isNightMode() ? neonColor : originalColor;
      paint.setColor(currentColor);
      paint.setAlpha((int) alpha); // restaura a opacidade do Fade In

      canvas.save();
      // variação suave de escala (±8%) baseada no tempo de vida do alvo
      float scale = 1.0f + (float) (Math.sin(lifetime * 5.0) * 0.08);
      canvas.scale(scale, scale, shape.centerX(), shape.centerY());
      super.draw(canvas);
      canvas.restore();
   }
}

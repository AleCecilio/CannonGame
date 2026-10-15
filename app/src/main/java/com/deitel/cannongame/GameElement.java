// GameElement.java
// Representa um elemento do jogo delimitado por um retângulo (Fig. 6.7)
package com.deitel.cannongame;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class GameElement {
   protected CannonView view; // a view que contém este GameElement
   protected Paint paint = new Paint(); // Paint usado para desenhar o elemento
   protected Rect shape; // limites retangulares do GameElement
   private float velocityY; // velocidade vertical do GameElement
   private int soundId; // som associado ao GameElement

   // construtor público
   public GameElement(CannonView view, int color, int soundId, int x,
      int y, int width, int length, float velocityY) {
      this.view = view;
      paint.setColor(color);
      shape = new Rect(x, y, x + width, y + length); // define os limites
      this.soundId = soundId;
      this.velocityY = velocityY;
   }

   // obtém a velocidade vertical
   public float getVelocityY() {
      return velocityY;
   }

   // define a velocidade vertical
   public void setVelocityY(float velocityY) {
      this.velocityY = velocityY;
   }

   // atualiza a posição do GameElement e verifica colisões com as paredes
   public void update(double interval) {
      // atualiza a posição vertical
      shape.offset(0, (int) (velocityY * interval));

      // se colidir com a parede (topo/base), inverte a direção
      if (shape.top < 0 && velocityY < 0 ||
         shape.bottom > view.getScreenHeight() && velocityY > 0)
         velocityY *= -1; // inverte a velocidade deste GameElement
   }

   // desenha este GameElement no Canvas recebido
   public void draw(Canvas canvas) {
      canvas.drawRect(shape, paint);
   }

   // reproduz o som correspondente a este tipo de GameElement
   public void playSound() {
      view.playSound(soundId);
   }
}

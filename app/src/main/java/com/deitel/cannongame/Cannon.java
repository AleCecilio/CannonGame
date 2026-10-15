// Cannon.java
// Representa o canhão e dispara a bala (Figs. 6.10–6.14)
package com.deitel.cannongame;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

public class Cannon {
   private int baseRadius; // raio da base do canhão
   private int barrelLength; // comprimento do cano
   private Point barrelEnd = new Point(); // extremidade do cano
   private double barrelAngle; // ângulo do cano
   private Cannonball cannonball; // a bala do canhão
   private Paint paint = new Paint(); // Paint usado para desenhar o canhão
   private CannonView view; // view que contém o canhão

   // construtor
   public Cannon(CannonView view, int baseRadius, int barrelLength,
      int barrelWidth) {
      this.view = view;
      this.baseRadius = baseRadius;
      this.barrelLength = barrelLength;
      paint.setStrokeWidth(barrelWidth); // define a espessura do cano
      paint.setColor(Color.BLACK); // a cor do canhão é preta
      align(Math.PI / 2); // cano inicialmente apontando para a direita
   }

   // alinha o cano do canhão ao ângulo recebido
   public void align(double barrelAngle) {
      this.barrelAngle = barrelAngle;
      barrelEnd.x = (int) (barrelLength * Math.sin(barrelAngle));
      barrelEnd.y = (int) (-barrelLength * Math.cos(barrelAngle)) +
         view.getScreenHeight() / 2;
   }

   // cria e dispara uma Cannonball na direção para a qual o canhão aponta
   public void fireCannonball() {
      // calcula o componente x da velocidade da bala
      int velocityX = (int) (CannonView.CANNONBALL_SPEED_PERCENT *
         view.getScreenWidth() * Math.sin(barrelAngle));

      // calcula o componente y da velocidade da bala
      int velocityY = (int) (CannonView.CANNONBALL_SPEED_PERCENT *
         view.getScreenWidth() * -Math.cos(barrelAngle));

      // calcula o raio da bala
      int radius = (int) (view.getScreenHeight() *
         CannonView.CANNONBALL_RADIUS_PERCENT);

      // constrói a Cannonball e a posiciona dentro do canhão
      cannonball = new Cannonball(view, Color.BLACK,
         CannonView.CANNON_SOUND_ID, -radius,
         view.getScreenHeight() / 2 - radius, radius, velocityX,
         velocityY);

      cannonball.playSound(); // reproduz o som do disparo
   }

   // desenha o canhão no Canvas
   public void draw(Canvas canvas) {
      // ajusta a cor conforme o tema (clara no modo noturno, preta no modo dia)
      paint.setColor(view.isNightMode() ? Color.LTGRAY : Color.BLACK);

      // desenha o cano do canhão
      canvas.drawLine(0, view.getScreenHeight() / 2, barrelEnd.x,
         barrelEnd.y, paint);

      // desenha a base do canhão
      canvas.drawCircle(0, (int) view.getScreenHeight() / 2,
         (int) baseRadius, paint);
   }

   // retorna a Cannonball disparada por este canhão
   public Cannonball getCannonball() {
      return cannonball;
   }

   // remove a Cannonball do jogo
   public void removeCannonball() {
      cannonball = null;
   }
}

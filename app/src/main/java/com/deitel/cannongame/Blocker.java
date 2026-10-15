// Blocker.java
// Subclasse de GameElement personalizada para o Blocker (Fig. 6.8)
package com.deitel.cannongame;

import android.graphics.Canvas;
import android.graphics.Color;

public class Blocker extends GameElement {
   private int missPenalty; // penalidade de erro deste Blocker

   // construtor
   public Blocker(CannonView view, int color, int missPenalty, int x,
      int y, int width, int length, float velocityY) {
      super(view, color, CannonView.BLOCKER_SOUND_ID, x, y, width, length,
         velocityY);
      this.missPenalty = missPenalty;
   }

   // retorna a penalidade de erro deste Blocker
   public int getMissPenalty() {
      return missPenalty;
   }

   // desenha o Blocker ajustando a cor conforme o modo de tema
   @Override
   public void draw(Canvas canvas) {
      paint.setColor(view.isNightMode() ? Color.LTGRAY : Color.BLACK);
      super.draw(canvas);
   }
}
